package ru.danilavak.zizu.binaryapi;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class BinaryEncodingWriter {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    void writeByte(int value) {
        outputStream.write(value & 0xFF);
    }

    void writeBytes(byte[] value) {
        outputStream.writeBytes(value);
    }

    void writeMagic(String value) {
        writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    void writeUInt16(int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("uint16 out of range: " + value);
        }
        writeByte(value >>> 8);
        writeByte(value);
    }

    void writeUInt32(long value) {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("uint32 out of range: " + value);
        }
        writeByte((int) (value >>> 24));
        writeByte((int) (value >>> 16));
        writeByte((int) (value >>> 8));
        writeByte((int) value);
    }

    void writeInt64(long value) {
        writeByte((int) (value >>> 56));
        writeByte((int) (value >>> 48));
        writeByte((int) (value >>> 40));
        writeByte((int) (value >>> 32));
        writeByte((int) (value >>> 24));
        writeByte((int) (value >>> 16));
        writeByte((int) (value >>> 8));
        writeByte((int) value);
    }

    void writeUuid(UUID value) {
        writeInt64(value.getMostSignificantBits());
        writeInt64(value.getLeastSignificantBits());
    }

    void writeUtf8String(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeUInt32(bytes.length);
        writeBytes(bytes);
    }

    void writeByteArray(byte[] value) {
        writeUInt32(value.length);
        writeBytes(value);
    }

    byte[] toByteArray() {
        return outputStream.toByteArray();
    }
}
