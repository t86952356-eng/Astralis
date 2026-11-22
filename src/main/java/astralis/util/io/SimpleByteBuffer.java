package astralis.util.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class SimpleByteBuffer {
    private byte[] data;
    private int markPosition;
    private int position;
    private int limit;
    private int capacity;
    private boolean bigEndian;
    private final boolean autoExpand;

    private SimpleByteBuffer(int initialCapacity) {
        this(initialCapacity, false);
    }

    private SimpleByteBuffer(int initialCapacity, boolean expandable) {

        this.markPosition = -1;
        this.position = 0;
        this.bigEndian = true;
        if (initialCapacity < 0) {

            throw new IllegalArgumentException("Capacity cannot be negative: " + initialCapacity);
        }

        this.capacity = initialCapacity;
        this.limit = initialCapacity;
        this.data = new byte[initialCapacity];
        this.autoExpand = expandable;
    }

    public byte getByte(int index) {
        this.checkIndex(index);
        return this.data[index];
    }

    private static int bytesToInt(byte b0, byte b1, byte b2, byte b3) {

        int unsignedB0 = b0 & 255;

        int shiftedB0 = unsignedB0 << 24;

        int unsignedB1 = b1 & 255;

        int combined01 = shiftedB0 | unsignedB1 << 16;

        int unsignedB2 = b2 & 255;

        int combined012 = combined01 | unsignedB2 << 8;

        return combined012 | b3 & 0xFF;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= this.limit) {

            throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
        }
    }

    public static SimpleByteBuffer allocate(int capacity) {
        return new SimpleByteBuffer(capacity);
    }

    private void checkBounds(int offset, int length, int arrayLength) {
        if ((offset | length | offset + length | arrayLength - (offset + length)) < 0) {
            throw new IndexOutOfBoundsException();
        }
    }

    public int getPosition() {
        return this.position;
    }

    public SimpleByteBuffer readBytes(byte[] destination, int offset, int length) {
        this.checkBounds(offset, length, destination.length);
        if (length > this.getRemaining()) {

            throw new IndexOutOfBoundsException("Buffer underflow");
        } else {
            System.arraycopy(this.data, this.position, destination, offset, length);
            this.position += length;
            return this;
        }
    }

    public int getCapacity() {
        return this.capacity;
    }

    public long readLong() {
        int remaining = this.getRemaining();

        if (remaining < 8) {

            throw new IndexOutOfBoundsException("Buffer underflow");
        }

        long result;
        if (this.bigEndian) {
            byte byte0 = this.data[this.position];
            byte byte1 = this.data[this.position + 1];
            byte byte2 = this.data[this.position + 2];
            byte byte3 = this.data[this.position + 3];
            byte byte4 = this.data[this.position + 4];
            byte byte5 = this.data[this.position + 5];

            byte byte6 = this.data[this.position + 6];

            result =
                    bytesToLong(
                            byte0,
                            byte1,
                            byte2,
                            byte3,
                            byte4,
                            byte5,
                            byte6,
                            this.data[this.position + 7]);
        } else {

            byte highByte = this.data[this.position + 7];

            result =
                    bytesToLong(
                            highByte,
                            this.data[this.position + 6],
                            this.data[this.position + 5],
                            this.data[this.position + 4],
                            this.data[this.position + 3],
                            this.data[this.position + 2],
                            this.data[this.position + 1],
                            this.data[this.position]);
        }

        this.position += 8;
        return result;
    }

    public int getLimit() {
        return this.limit;
    }

    public boolean hasRemaining() {
        return this.position < this.limit;
    }

    private static long bytesToLong(
            byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
        long result;
        long part;
        result = b0;

        result &= 255L;

        result <<= 56;
        part = b1;

        part &= 255L;

        result |= part << 48;
        part = b2;

        part &= 255L;

        result |= part << 40;
        part = b3;

        part &= 255L;

        result |= part << 32;
        part = b4;

        part &= 255L;

        result |= part << 24;
        part = b5;

        part &= 255L;

        result |= part << 16;
        part = b6;

        part &= 255L;

        result |= part << 8;
        part = b7;

        return result | part & 255L;
    }

    public SimpleByteBuffer setPosition(int newPosition) {
        if (newPosition <= this.limit && newPosition >= 0) {
            this.position = newPosition;
            if (this.markPosition > this.position) {
                this.markPosition = -1;
            }

            return this;
        } else {

            throw new IllegalArgumentException("Bad position");
        }
    }

    public static SimpleByteBuffer fromByteBuffer(ByteBuffer source) {
        if (source == null) {
            throw new NullPointerException();
        }

        SimpleByteBuffer result = allocate(source.remaining());
        result.writeByteBuffer(source.duplicate());
        result.flip();
        return result;
    }

    public SimpleByteBuffer mark() {
        this.markPosition = this.position;
        return this;
    }

    public SimpleByteBuffer readBytes(byte[] destination) {
        return this.readBytes(destination, 0, destination.length);
    }

    public SimpleByteBuffer rewind() {
        this.position = 0;
        this.markPosition = -1;
        return this;
    }

    public static SimpleByteBuffer wrap(byte[] bytes) {
        SimpleByteBuffer buffer = new SimpleByteBuffer(bytes.length);
        buffer.data = bytes;
        return buffer;
    }

    public SimpleByteBuffer writeByteBuffer(ByteBuffer source) {
        int count = source.remaining();
        this.ensureWritable(count);
        source.get(this.data, this.position, count);
        this.position += count;
        return this;
    }

    public SimpleByteBuffer writeLong(long value) {

        this.ensureWritable(8);
        if (this.bigEndian) {
            byte[] target1 = this.data;
            int index1 = this.position++;

            target1[index1] = (byte) (value >> 56);
            byte[] target2 = this.data;
            int index2 = this.position++;

            target2[index2] = (byte) (value >> 48);
            byte[] target3 = this.data;
            int index3 = this.position++;

            target3[index3] = (byte) (value >> 40);
            byte[] target4 = this.data;
            int index4 = this.position++;

            target4[index4] = (byte) (value >> 32);
            byte[] target5 = this.data;
            int index5 = this.position++;

            target5[index5] = (byte) (value >> 24);
            byte[] target6 = this.data;
            int index6 = this.position++;

            target6[index6] = (byte) (value >> 16);
            byte[] target7 = this.data;
            int index7 = this.position++;

            target7[index7] = (byte) (value >> 8);
            this.data[this.position++] = (byte) value;
        } else {
            this.data[this.position++] = (byte) value;
            byte[] target1 = this.data;
            int index1 = this.position++;

            target1[index1] = (byte) (value >> 8);
            byte[] target2 = this.data;
            int index2 = this.position++;

            target2[index2] = (byte) (value >> 16);
            byte[] target3 = this.data;
            int index3 = this.position++;

            target3[index3] = (byte) (value >> 24);
            byte[] target4 = this.data;
            int index4 = this.position++;

            target4[index4] = (byte) (value >> 32);
            byte[] target5 = this.data;
            int index5 = this.position++;

            target5[index5] = (byte) (value >> 40);
            byte[] target6 = this.data;
            int index6 = this.position++;

            target6[index6] = (byte) (value >> 48);
            byte[] target7 = this.data;
            int index7 = this.position++;

            target7[index7] = (byte) (value >> 56);
        }

        return this;
    }

    public SimpleByteBuffer setByteOrder(ByteOrder byteOrder) {
        this.bigEndian = byteOrder == ByteOrder.BIG_ENDIAN;
        return this;
    }

    public SimpleByteBuffer writeInt(int value) {
        this.ensureWritable(4);
        if (this.bigEndian) {
            byte[] target1 = this.data;
            int index1 = this.position++;

            target1[index1] = (byte) (value >> 24);
            byte[] target2 = this.data;
            int index2 = this.position++;

            target2[index2] = (byte) (value >> 16);
            byte[] target3 = this.data;
            int index3 = this.position++;

            target3[index3] = (byte) (value >> 8);
            this.data[this.position++] = (byte) value;
        } else {
            this.data[this.position++] = (byte) value;
            byte[] target1 = this.data;
            int index1 = this.position++;

            target1[index1] = (byte) (value >> 8);
            byte[] target2 = this.data;
            int index2 = this.position++;

            target2[index2] = (byte) (value >> 16);
            byte[] target3 = this.data;
            int index3 = this.position++;

            target3[index3] = (byte) (value >> 24);
        }

        return this;
    }

    public int readInt() {
        if (this.getRemaining() < 4) {

            throw new IndexOutOfBoundsException("Buffer underflow");
        }

        int result;
        if (this.bigEndian) {
            result =
                    bytesToInt(
                            this.data[this.position],
                            this.data[this.position + 1],
                            this.data[this.position + 2],
                            this.data[this.position + 3]);
        } else {
            result =
                    bytesToInt(
                            this.data[this.position + 3],
                            this.data[this.position + 2],
                            this.data[this.position + 1],
                            this.data[this.position]);
        }

        this.position += 4;
        return result;
    }

    public static SimpleByteBuffer allocateExpandable(int initialCapacity) {
        return new SimpleByteBuffer(initialCapacity, true);
    }

    public SimpleByteBuffer setByte(int index, byte value) {
        this.checkIndex(index);
        this.data[index] = value;
        return this;
    }

    public void zeroFill() {
        Arrays.fill(this.data, (byte) 0);
    }

    private void ensureWritable(int requiredBytes) {
        if (requiredBytes > this.getRemaining()) {
            if (!this.autoExpand) {

                throw new IndexOutOfBoundsException("Buffer overflow");
            }

            int newCapacity = Math.max(this.capacity * 2, this.position + requiredBytes);
            byte[] newData = new byte[newCapacity];
            System.arraycopy(this.data, 0, newData, 0, this.position);
            this.data = newData;
            this.capacity = newCapacity;
            this.limit = newCapacity;
        }
    }

    public ByteOrder getByteOrder() {
        return this.bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    public SimpleByteBuffer writeBytes(byte[] bytes, int offset, int length) {
        this.checkBounds(offset, length, bytes.length);
        this.ensureWritable(length);
        System.arraycopy(bytes, offset, this.data, this.position, length);
        this.position += length;
        return this;
    }

    public SimpleByteBuffer flip() {
        this.limit = this.position;
        this.position = 0;
        this.markPosition = -1;
        return this;
    }

    public byte[] getArray() {
        return this.data;
    }

    public int getRemaining() {
        return this.limit - this.position;
    }

    public SimpleByteBuffer writeBytes(byte[] bytes) {
        return this.writeBytes(bytes, 0, bytes.length);
    }

    public int getInt(int index) {
        this.checkBounds(index, 4, this.capacity);
        return this.bigEndian
                ? bytesToInt(
                        this.data[index],
                        this.data[index + 1],
                        this.data[index + 2],
                        this.data[index + 3])
                : bytesToInt(
                        this.data[index + 3],
                        this.data[index + 2],
                        this.data[index + 1],
                        this.data[index]);
    }

    public SimpleByteBuffer setLimit(int newLimit) {
        if (newLimit <= this.capacity && newLimit >= 0) {
            this.limit = newLimit;
            if (this.position > this.limit) {
                this.position = this.limit;
            }

            if (this.markPosition > this.limit) {
                this.markPosition = -1;
            }

            return this;
        } else {

            throw new IllegalArgumentException("Bad limit");
        }
    }

    public SimpleByteBuffer resetToMark() {
        if (this.markPosition < 0) {

            throw new IllegalStateException("Mark has not been defined");
        } else {
            this.position = this.markPosition;
            return this;
        }
    }

    public byte readByte() {
        if (this.position >= this.limit) {

            throw new IndexOutOfBoundsException("Buffer underflow");
        } else {
            return this.data[this.position++];
        }
    }

    public SimpleByteBuffer compact() {
        int remaining = this.getRemaining();
        if (remaining > 0) {
            System.arraycopy(this.data, this.position, this.data, 0, remaining);
        }

        this.position = remaining;
        this.limit = this.capacity;
        this.markPosition = -1;
        return this;
    }

    public SimpleByteBuffer writeByte(byte value) {
        this.ensureWritable(1);
        this.data[this.position++] = value;
        return this;
    }

    public SimpleByteBuffer clear() {
        this.position = 0;
        this.limit = this.capacity;
        this.markPosition = -1;
        return this;
    }

    public SimpleByteBuffer writeBuffer(SimpleByteBuffer source) {
        if (source == this) {

            throw new IllegalArgumentException("The source buffer cannot be this buffer");
        } else {
            int count = source.getRemaining();
            this.ensureWritable(count);
            System.arraycopy(source.data, source.position, this.data, this.position, count);
            source.position += count;
            this.position += count;
            return this;
        }
    }
}
