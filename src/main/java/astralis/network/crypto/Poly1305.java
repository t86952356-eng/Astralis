package astralis.network.crypto;

public final class Poly1305 {
    private final long r0;
    private final long r1;
    private final long r2;
    private final long r3;
    private final long r4;
    private final long s1;
    private final long s2;
    private final long s3;
    private final long s4;
    private final long pad0;
    private final long pad1;
    private final long pad2;
    private final long pad3;
    private long h0;
    private long h1;
    private long h2;
    private long h3;
    private long h4;

    Poly1305(byte[] key) {
        long limb;
        super();
        long keyWord0 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 0);
        long keyWord1 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 4);

        long keyWord2 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 8);

        long keyWord3 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 12);

        this.r0 = keyWord0 & 67108863L;

        limb = keyWord0 >>> 26;

        limb |= keyWord1 << 6;

        this.r1 = limb & 67108611L;

        limb = keyWord1 >>> 20;

        limb |= keyWord2 << 12;

        this.r2 = limb & 67092735L;

        limb = keyWord2 >>> 14;

        limb |= keyWord3 << 18;

        this.r3 = limb & 66076671L;

        limb = keyWord3 >>> 8;

        this.r4 = limb & 1048575L;

        this.s1 = this.r1 * 5L;

        this.s2 = this.r2 * 5L;

        this.s3 = this.r3 * 5L;

        this.s4 = this.r4 * 5L;

        this.pad0 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 16);

        this.pad1 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 20);

        this.pad2 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 24);

        this.pad3 = ChaCha20Poly1305.readUnsignedIntLittleEndian(key, 28);
    }

    byte[] finishTag() {
        long word;
        long sum;

        long carry = this.h1 >>> 26;

        this.h1 &= 67108863L;
        this.h2 += carry;

        carry = this.h2 >>> 26;

        this.h2 &= 67108863L;
        this.h3 += carry;

        carry = this.h3 >>> 26;

        this.h3 &= 67108863L;
        this.h4 += carry;

        carry = this.h4 >>> 26;

        this.h4 &= 67108863L;

        this.h0 += carry * 5L;

        carry = this.h0 >>> 26;

        this.h0 &= 67108863L;
        this.h1 += carry;

        long g0 = this.h0 + 5L;

        carry = g0 >>> 26;

        g0 &= 67108863L;
        long g1 = this.h1 + carry;

        carry = g1 >>> 26;

        g1 &= 67108863L;
        long g2 = this.h2 + carry;

        carry = g2 >>> 26;

        g2 &= 67108863L;
        long g3 = this.h3 + carry;

        carry = g3 >>> 26;

        g3 &= 67108863L;
        sum = this.h4 + carry;

        long g4 = sum - 67108864L;

        long selectMask = (g4 >>> 63) - 1L;
        g0 &= selectMask;
        g1 &= selectMask;
        g2 &= selectMask;
        g3 &= selectMask;
        g4 &= selectMask;

        long inverseMask = ~selectMask;
        this.h0 = this.h0 & inverseMask | g0;
        this.h1 = this.h1 & inverseMask | g1;
        this.h2 = this.h2 & inverseMask | g2;
        this.h3 = this.h3 & inverseMask | g3;
        this.h4 = this.h4 & inverseMask | g4;

        word = this.h0 | this.h1 << 26;

        long word0 = word & 4294967295L;

        word = this.h1 >>> 6;

        word |= this.h2 << 20;

        long word1 = word & 4294967295L;

        word = this.h2 >>> 12;

        word |= this.h3 << 14;

        long word2 = word & 4294967295L;

        word = this.h3 >>> 18;

        word |= this.h4 << 8;

        long word3 = word & 4294967295L;
        byte[] tag = new byte[16];
        long accumulator = word0 + this.pad0;
        ChaCha20Poly1305.writeIntLittleEndian(tag, 0, (int) accumulator);

        long addCarry = accumulator >>> 32;
        accumulator = word1 + this.pad1 + addCarry;
        ChaCha20Poly1305.writeIntLittleEndian(tag, 4, (int) accumulator);

        addCarry = accumulator >>> 32;
        accumulator = word2 + this.pad2 + addCarry;

        ChaCha20Poly1305.writeIntLittleEndian(tag, 8, (int) accumulator);

        addCarry = accumulator >>> 32;
        accumulator = word3 + this.pad3 + addCarry;

        ChaCha20Poly1305.writeIntLittleEndian(tag, 12, (int) accumulator);
        return tag;
    }

    void processBlock(byte[] data, int offset, long hibit) {
        long word0 = ChaCha20Poly1305.readUnsignedIntLittleEndian(data, offset);
        long word1 = ChaCha20Poly1305.readUnsignedIntLittleEndian(data, offset + 4);

        long limb;
        long word2 = ChaCha20Poly1305.readUnsignedIntLittleEndian(data, offset + 8);

        long word3 = ChaCha20Poly1305.readUnsignedIntLittleEndian(data, offset + 12);

        this.h0 += word0 & 67108863L;

        long highBits = word0 >>> 26;

        long combinedLimb = highBits | word1 << 6;

        this.h1 += combinedLimb & 67108863L;

        limb = word1 >>> 20;

        limb |= word2 << 12;

        this.h2 += limb & 67108863L;

        limb = word2 >>> 14;

        limb |= word3 << 18;

        this.h3 += limb & 67108863L;

        this.h4 += (word3 >>> 8) + hibit;
        long d0 =
                this.h0 * this.r0
                        + this.h1 * this.s4
                        + this.h2 * this.s3
                        + this.h3 * this.s2
                        + this.h4 * this.s1;
        long d1 =
                this.h0 * this.r1
                        + this.h1 * this.r0
                        + this.h2 * this.s4
                        + this.h3 * this.s3
                        + this.h4 * this.s2;
        long d2 =
                this.h0 * this.r2
                        + this.h1 * this.r1
                        + this.h2 * this.r0
                        + this.h3 * this.s4
                        + this.h4 * this.s3;
        long d3 =
                this.h0 * this.r3
                        + this.h1 * this.r2
                        + this.h2 * this.r1
                        + this.h3 * this.r0
                        + this.h4 * this.s4;
        long d4 =
                this.h0 * this.r4
                        + this.h1 * this.r3
                        + this.h2 * this.r2
                        + this.h3 * this.r1
                        + this.h4 * this.r0;
        long carry = d0 >>> 26;

        this.h0 = d0 & 67108863L;
        d1 += carry;
        carry = d1 >>> 26;

        this.h1 = d1 & 67108863L;
        d2 += carry;
        carry = d2 >>> 26;

        this.h2 = d2 & 67108863L;
        d3 += carry;
        carry = d3 >>> 26;

        this.h3 = d3 & 67108863L;
        d4 += carry;
        carry = d4 >>> 26;

        this.h4 = d4 & 67108863L;

        this.h0 += carry * 5L;
        carry = this.h0 >>> 26;

        this.h0 &= 67108863L;
        this.h1 += carry;
    }

    void processData(byte[] data, int offset, int length) {

        int fullBlockBytes = length & -16;
        int position = 0;

        while (position < fullBlockBytes) {
            int blockStart = offset + position;

            this.processBlock(data, blockStart, 16777216L);
            position += 16;
        }

        position = length - fullBlockBytes;
        if (position > 0) {

            byte[] lastBlock = new byte[16];
            System.arraycopy(data, offset + fullBlockBytes, lastBlock, 0, position);

            this.processBlock(lastBlock, 0, 16777216L);
        }
    }
}
