package astralis.scripting;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Set;

public class ScriptClassValidator extends ClassVisitor {
    final Set val$scriptClasses;

    ScriptClassValidator(int api, Set scriptClasses) {
        super(api);
        this.val$scriptClasses = scriptClasses;
    }

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {
        ScriptManager.validateClassReference(superName, this.val$scriptClasses);
        if (interfaces != null) {
            for (String interfaceName : interfaces) {
                ScriptManager.validateClassReference(interfaceName, this.val$scriptClasses);
            }
        }

        ScriptManager.validateSignature(signature, false, this.val$scriptClasses);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {

        if ((access & 256) != 0) {
            throw new IllegalArgumentException("Native methods are not allowed in scripts.");
        }

        ScriptManager.validateTypeReference(Type.getReturnType(descriptor), this.val$scriptClasses);

        for (Type argumentType : Type.getArgumentTypes(descriptor)) {
            ScriptManager.validateTypeReference(argumentType, this.val$scriptClasses);
        }

        ScriptManager.validateSignature(signature, false, this.val$scriptClasses);
        if (exceptions != null) {
            for (String exception : exceptions) {
                ScriptManager.validateClassReference(exception, this.val$scriptClasses);
            }
        }

        return new ScriptMethodValidator(this, 589824, this.val$scriptClasses);
    }

    @Override
    public FieldVisitor visitField(
            int access, String name, String descriptor, String signature, Object value) {
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
        ScriptManager.validateSignature(signature, true, this.val$scriptClasses);

        return new ScriptFieldValidator(this, 589824, this.val$scriptClasses);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
        return ScriptManager.createAnnotationValidator(this.val$scriptClasses);
    }
}
