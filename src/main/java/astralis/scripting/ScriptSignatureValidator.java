package astralis.scripting;

import org.objectweb.asm.signature.SignatureVisitor;

import java.util.Set;

public class ScriptSignatureValidator extends SignatureVisitor {
    final Set val$scriptClasses;

    ScriptSignatureValidator(int api, Set scriptClasses) {
        super(api);
        this.val$scriptClasses = scriptClasses;
    }

    @Override
    public void visitClassType(String name) {
        ScriptManager.validateClassReference(name, this.val$scriptClasses);
    }
}
