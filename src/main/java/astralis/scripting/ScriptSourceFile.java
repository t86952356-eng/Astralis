package astralis.scripting;

public record ScriptSourceFile(String relativePath, byte[] content) {
    public ScriptSourceFile(String relativePath, byte[] content) {
        content = (byte[]) content.clone();
        this.relativePath = relativePath;
        this.content = content;
    }

    public byte[] getContentCopy() {
        return (byte[]) this.content.clone();
    }

    public String getRelativePath() {
        return this.relativePath;
    }
}
