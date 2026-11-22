package astralis.scripting;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.Set;

public class ScriptAnnotationValidator extends AnnotationVisitor {
    final Set val$scriptClasses;

    ScriptAnnotationValidator(int api, Set scriptClasses) {
        super(api);
        this.val$scriptClasses = scriptClasses;
    }

    @Override
    public void visit(String name, Object value) {
        if (value instanceof Type type) {
            ScriptManager.validateTypeReference(type, this.val$scriptClasses);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
        return this;
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return this;
    }
}
