package astralis.account;

public record DecryptResult(byte[] data, boolean resaveRequired) {

    public boolean isResaveRequired() {
        return this.resaveRequired;
    }

    public byte[] getData() {
        return this.data;
    }
}
