package astralis.scripting;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ScriptMethodValidator extends MethodVisitor {
    final Set val$scriptClasses;

    ScriptMethodValidator(ScriptClassValidator classValidator, int asmApi, Set scriptClasses) {
        this.val$scriptClasses = scriptClasses;
        Objects.requireNonNull(classValidator);
        super(asmApi);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
        return ScriptManager.createAnnotationValidator(this.val$scriptClasses);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (type.startsWith("[")) {
            ScriptManager.validateTypeReference(Type.getType(type), this.val$scriptClasses);
        } else {
            ScriptManager.validateClassReference(type, this.val$scriptClasses);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        ScriptManager.validateClassReference(owner, this.val$scriptClasses);
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
    }

    @Override
    public void visitMethodInsn(
            int opcode, String owner, String name, String descriptor, boolean isInterface) {

        ScriptManager.validateClassReference(owner, this.val$scriptClasses);
        ScriptManager.validateTypeReference(Type.getReturnType(descriptor), this.val$scriptClasses);

        for (Type argumentType : Type.getArgumentTypes(descriptor)) {
            ScriptManager.validateTypeReference(argumentType, this.val$scriptClasses);
        }

        if (!name.equals("getClass")) {

            if (!List.of("getStackTrace", "printStackTrace", "fillInStackTrace").contains(name)) {
                return;
            }
        }

        throw new IllegalArgumentException(
                "Scripts cannot inspect runtime classes or process stack traces.");
    }

    @Override
    public void visitInvokeDynamicInsn(
            String name,
            String descriptor,
            Handle bootstrapMethodHandle,
            Object... bootstrapArguments) {

        if (!Set.of(
                        "java/lang/invoke/LambdaMetafactory",
                        "java/lang/invoke/StringConcatFactory",
                        "java/lang/runtime/ObjectMethods")
                .contains(bootstrapMethodHandle.getOwner())) {

            throw new IllegalArgumentException("Unsupported dynamic bytecode bootstrap.");
        }

        ScriptManager.validateTypeReference(Type.getReturnType(descriptor), this.val$scriptClasses);

        for (Type argumentType : Type.getArgumentTypes(descriptor)) {
            ScriptManager.validateTypeReference(argumentType, this.val$scriptClasses);
        }

        for (Object bootstrapArgument : bootstrapArguments) {
            if (bootstrapArgument instanceof Type typeArgument) {
                ScriptManager.validateTypeReference(typeArgument, this.val$scriptClasses);
            } else if (bootstrapArgument instanceof Handle handleArgument) {
                ScriptManager.validateHandle(handleArgument, this.val$scriptClasses);
            } else if (bootstrapArgument instanceof ConstantDynamic) {

                throw new IllegalArgumentException("Dynamic constants are not allowed in scripts.");
            }
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof Type typeValue) {
            ScriptManager.validateTypeReference(typeValue, this.val$scriptClasses);
        } else if (value instanceof Handle handleValue) {
            ScriptManager.validateHandle(handleValue, this.val$scriptClasses);
        } else if (value instanceof ConstantDynamic) {

            throw new IllegalArgumentException("Dynamic constants are not allowed in scripts.");
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (type != null) {
            ScriptManager.validateClassReference(type, this.val$scriptClasses);
        }
    }
}
