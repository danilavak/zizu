package ru.danilavak.zizu.binaryapi;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.danilavak.zizu.malware.MalwareSignatureService;
import ru.danilavak.zizu.malware.SignatureStatus;
import ru.danilavak.zizu.signature.DigitalSignatureService;

@Service
public class BinarySignatureExportService {
    private static final int EXPORT_TYPE_FULL = 1;
    private static final int EXPORT_TYPE_INCREMENT = 2;
    private static final int EXPORT_TYPE_BY_IDS = 3;

    private final MalwareSignatureService malwareSignatureService;
    private final DigitalSignatureService digitalSignatureService;
    private final BinaryApiProperties properties;

    public BinarySignatureExportService(
            MalwareSignatureService malwareSignatureService,
            DigitalSignatureService digitalSignatureService,
            BinaryApiProperties properties
    ) {
        this.malwareSignatureService = malwareSignatureService;
        this.digitalSignatureService = digitalSignatureService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public BinaryExportPayload exportFull() {
        return buildExportPayload(
                malwareSignatureService.getFullDatabase(),
                EXPORT_TYPE_FULL,
                null
        );
    }

    @Transactional(readOnly = true)
    public BinaryExportPayload exportIncrement(Instant since) {
        return buildExportPayload(
                malwareSignatureService.getIncrement(since),
                EXPORT_TYPE_INCREMENT,
                since
        );
    }

    @Transactional(readOnly = true)
    public BinaryExportPayload exportByIds(List<UUID> ids) {
        return buildExportPayload(
                malwareSignatureService.getByIds(ids),
                EXPORT_TYPE_BY_IDS,
                null
        );
    }

    private BinaryExportPayload buildExportPayload(
            List<MalwareSignatureService.SignatureResponse> signatures,
            int exportType,
            Instant since
    ) {
        byte[] dataBytes = buildDataBytes(signatures);
        byte[] manifestUnsignedBytes = buildManifestUnsignedBytes(signatures, exportType, since, dataBytes);
        byte[] manifestSignatureBytes = digitalSignatureService.signRawBytes(manifestUnsignedBytes);

        BinaryEncodingWriter manifestWriter = new BinaryEncodingWriter();
        manifestWriter.writeBytes(manifestUnsignedBytes);
        manifestWriter.writeUInt32(manifestSignatureBytes.length);
        manifestWriter.writeBytes(manifestSignatureBytes);

        Map<String, byte[]> parts = new LinkedHashMap<>();
        parts.put("manifest.bin", manifestWriter.toByteArray());
        parts.put("data.bin", dataBytes);
        return new BinaryExportPayload(parts, manifestUnsignedBytes, manifestSignatureBytes);
    }

    private byte[] buildManifestUnsignedBytes(
            List<MalwareSignatureService.SignatureResponse> signatures,
            int exportType,
            Instant since,
            byte[] dataBytes
    ) {
        BinaryEncodingWriter writer = new BinaryEncodingWriter();
        writer.writeMagic("MF-" + properties.normalizedSurname());
        writer.writeUInt16(properties.resolvedVersion());
        writer.writeByte(exportType);
        writer.writeInt64(Instant.now().toEpochMilli());
        writer.writeInt64(since == null ? -1L : since.toEpochMilli());
        writer.writeUInt32(signatures.size());
        writer.writeBytes(sha256(dataBytes));

        long currentOffset = 0L;
        for (MalwareSignatureService.SignatureResponse signature : signatures) {
            byte[] recordBytes = buildDataRecordBytes(signature);
            byte[] recordSignatureBytes = java.util.Base64.getDecoder().decode(signature.digitalSignatureBase64());

            writer.writeUuid(signature.id());
            writer.writeByte(toStatusCode(signature.status()));
            writer.writeInt64(signature.updatedAt().toEpochMilli());
            writer.writeInt64(currentOffset);
            writer.writeUInt32(recordBytes.length);
            writer.writeUInt32(recordSignatureBytes.length);
            writer.writeBytes(recordSignatureBytes);

            currentOffset += recordBytes.length;
        }

        return writer.toByteArray();
    }

    private byte[] buildDataBytes(List<MalwareSignatureService.SignatureResponse> signatures) {
        BinaryEncodingWriter writer = new BinaryEncodingWriter();
        writer.writeMagic("DB-" + properties.normalizedSurname());
        writer.writeUInt16(properties.resolvedVersion());
        writer.writeUInt32(signatures.size());
        for (MalwareSignatureService.SignatureResponse signature : signatures) {
            writer.writeBytes(buildDataRecordBytes(signature));
        }
        return writer.toByteArray();
    }

    private byte[] buildDataRecordBytes(MalwareSignatureService.SignatureResponse signature) {
        BinaryEncodingWriter writer = new BinaryEncodingWriter();
        writer.writeUtf8String(signature.threatName());
        writer.writeByteArray(hexToBytes(signature.firstBytesHex()));
        writer.writeByteArray(hexToBytes(signature.remainderHashHex()));
        writer.writeInt64(signature.remainderLength());
        writer.writeUtf8String(signature.fileType());
        writer.writeInt64(signature.offsetStart());
        writer.writeInt64(signature.offsetEnd());
        return writer.toByteArray();
    }

    private byte[] sha256(byte[] payload) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(payload);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private byte[] hexToBytes(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return result;
    }

    private int toStatusCode(SignatureStatus status) {
        return status == SignatureStatus.DELETED ? 2 : 1;
    }

    public record BinaryExportPayload(
            Map<String, byte[]> parts,
            byte[] unsignedManifest,
            byte[] manifestSignature
    ) {
    }
}
