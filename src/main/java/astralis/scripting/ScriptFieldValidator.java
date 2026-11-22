package astralis.scripting;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;

import java.util.Objects;
import java.util.Set;

public class ScriptFieldValidator extends FieldVisitor {
    final Set val$scriptClasses;

    ScriptFieldValidator(ScriptClassValidator classValidator, int api, Set scriptClasses) {
        this.val$scriptClasses = scriptClasses;
        Objects.requireNonNull(classValidator);
        super(api);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        ScriptManager.validateTypeReference(Type.getType(descriptor), this.val$scriptClasses);
        return ScriptManager.createAnnotationValidator(this.val$scriptClasses);
    }
}
