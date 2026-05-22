package ru.danilavak.zizu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.danilavak.zizu.malware.MalwareSignatureAuditRepository;
import ru.danilavak.zizu.malware.MalwareSignatureFileRepository;
import ru.danilavak.zizu.malware.MalwareSignatureHistoryRepository;
import ru.danilavak.zizu.malware.MalwareSignatureRepository;
import ru.danilavak.zizu.model.UserRole;
import ru.danilavak.zizu.service.UserAccountService;
import ru.danilavak.zizu.signature.DigitalSignatureService;

@SpringBootTest
@AutoConfigureMockMvc
class BinarySignatureExportIntegrationTests {
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("boundary=\"?([^\";]+)\"?");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private MalwareSignatureRepository malwareSignatureRepository;

    @Autowired
    private MalwareSignatureHistoryRepository malwareSignatureHistoryRepository;

    @Autowired
    private MalwareSignatureAuditRepository malwareSignatureAuditRepository;

    @Autowired
    private MalwareSignatureFileRepository malwareSignatureFileRepository;

    @Autowired
    private DigitalSignatureService digitalSignatureService;

    @BeforeEach
    void clearSignatureState() {
        malwareSignatureAuditRepository.deleteAll();
        malwareSignatureHistoryRepository.deleteAll();
        malwareSignatureFileRepository.deleteAll();
        malwareSignatureRepository.deleteAll();
    }

    @Test
    void exportsFullManifestAndDataMultipart() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = registerAndLogin("bin_admin_" + suffix, "bin_admin_" + suffix + "@example.com", "Admin!234", UserRole.ADMIN);
        String userToken = registerAndLogin("bin_user_" + suffix, "bin_user_" + suffix + "@example.com", "User!234", UserRole.USER);

        String createResponse = createSignature(adminToken, """
                {
                  "threatName": "Spyware.%s",
                  "firstBytesHex": "AA55CC33",
                  "remainderHashHex": "0123456789ABCDEF",
                  "remainderLength": 512,
                  "fileType": "exe",
                  "offsetStart": 4,
                  "offsetEnd": 128
                }
                """.formatted(suffix));

        JsonNode created = objectMapper.readTree(createResponse);
        UUID signatureId = UUID.fromString(created.get("id").asText());
        Instant updatedAt = Instant.parse(created.get("updatedAt").asText());
        String recordSignatureBase64 = created.get("digitalSignatureBase64").asText();

        MockHttpServletResponse response = mockMvc.perform(get("/api/binary/signatures/full")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        MultipartParts parts = parseMultipart(response);
        assertThat(parts.parts()).containsKeys("manifest.bin", "data.bin");

        byte[] dataBytes = parts.parts().get("data.bin");
        byte[] manifestBytes = parts.parts().get("manifest.bin");

        ParsedData parsedData = parseData(dataBytes);
        assertThat(parsedData.magic()).isEqualTo("DB-VAK");
        assertThat(parsedData.version()).isEqualTo(1);
        assertThat(parsedData.recordCount()).isEqualTo(1);
        assertThat(parsedData.records()).hasSize(1);
        ParsedDataRecord record = parsedData.records().getFirst();
        assertThat(record.threatName()).isEqualTo("Spyware." + suffix);
        assertThat(record.firstBytes()).isEqualTo(hex("AA55CC33"));
        assertThat(record.remainderHash()).isEqualTo(hex("0123456789ABCDEF"));
        assertThat(record.remainderLength()).isEqualTo(512L);
        assertThat(record.fileType()).isEqualTo("exe");
        assertThat(record.offsetStart()).isEqualTo(4L);
        assertThat(record.offsetEnd()).isEqualTo(128L);

        ParsedManifest parsedManifest = parseManifest(manifestBytes, 1);
        assertThat(parsedManifest.magic()).isEqualTo("MF-VAK");
        assertThat(parsedManifest.version()).isEqualTo(1);
        assertThat(parsedManifest.exportType()).isEqualTo(1);
        assertThat(parsedManifest.sinceEpochMillis()).isEqualTo(-1L);
        assertThat(parsedManifest.recordCount()).isEqualTo(1);
        assertThat(parsedManifest.dataSha256()).isEqualTo(MessageDigest.getInstance("SHA-256").digest(dataBytes));
        assertThat(parsedManifest.entries()).hasSize(1);
        ParsedManifestEntry entry = parsedManifest.entries().getFirst();
        assertThat(entry.id()).isEqualTo(signatureId);
        assertThat(entry.statusCode()).isEqualTo(1);
        assertThat(entry.updatedAtEpochMillis()).isEqualTo(updatedAt.toEpochMilli());
        assertThat(entry.dataOffset()).isEqualTo(0L);
        assertThat(entry.dataLength()).isEqualTo(parsedData.recordByteLengths().getFirst().longValue());
        assertThat(entry.recordSignatureBytes()).isEqualTo(Base64.getDecoder().decode(recordSignatureBase64));
        assertThat(digitalSignatureService.verifyRawBytes(parsedManifest.unsignedBytes(), parsedManifest.manifestSignatureBytes())).isTrue();
    }

    @Test
    void exportsIncrementAndByIdsUsingMultipart() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = registerAndLogin("bin2_admin_" + suffix, "bin2_admin_" + suffix + "@example.com", "Admin!234", UserRole.ADMIN);
        String userToken = registerAndLogin("bin2_user_" + suffix, "bin2_user_" + suffix + "@example.com", "User!234", UserRole.USER);
        Instant since = Instant.now().minus(1, ChronoUnit.MINUTES);

        String createResponse = createSignature(adminToken, """
                {
                  "threatName": "Rootkit.%s",
                  "firstBytesHex": "ABCD1234",
                  "remainderHashHex": "1111222233334444",
                  "remainderLength": 256,
                  "fileType": "dll",
                  "offsetStart": 0,
                  "offsetEnd": 64
                }
                """.formatted(suffix));
        UUID signatureId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        MockHttpServletResponse byIdsResponse = mockMvc.perform(post("/api/binary/signatures/by-ids")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "ids": ["%s", "%s"] }
                                """.formatted(signatureId, UUID.randomUUID())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        ParsedManifest byIdsManifest = parseManifest(parseMultipart(byIdsResponse).parts().get("manifest.bin"), 1);
        assertThat(byIdsManifest.exportType()).isEqualTo(3);
        assertThat(byIdsManifest.recordCount()).isEqualTo(1);
        assertThat(byIdsManifest.entries().getFirst().id()).isEqualTo(signatureId);

        mockMvc.perform(delete("/malware-signatures/{id}", signatureId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        MockHttpServletResponse incrementResponse = mockMvc.perform(get("/api/binary/signatures/increment")
                        .header("Authorization", "Bearer " + userToken)
                        .param("since", since.toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        ParsedManifest incrementManifest = parseManifest(parseMultipart(incrementResponse).parts().get("manifest.bin"), 1);
        assertThat(incrementManifest.exportType()).isEqualTo(2);
        assertThat(incrementManifest.sinceEpochMillis()).isEqualTo(since.toEpochMilli());
        assertThat(incrementManifest.entries()).hasSize(1);
        assertThat(incrementManifest.entries().getFirst().statusCode()).isEqualTo(2);

        mockMvc.perform(get("/api/binary/signatures/increment")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    private String registerAndLogin(String username, String email, String password, UserRole role) throws Exception {
        userAccountService.register(username, email, password, role);
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createSignature(String adminToken, String payload) throws Exception {
        return mockMvc.perform(post("/malware-signatures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private MultipartParts parseMultipart(MockHttpServletResponse response) throws Exception {
        String contentType = response.getContentType();
        Matcher matcher = BOUNDARY_PATTERN.matcher(contentType);
        assertThat(matcher.find()).isTrue();
        String boundary = matcher.group(1);
        String body = response.getContentAsString();
        String[] chunks = body.split("--" + Pattern.quote(boundary));
        java.util.LinkedHashMap<String, byte[]> parts = new java.util.LinkedHashMap<>();
        for (String chunk : chunks) {
            if (chunk.isBlank() || chunk.equals("--") || chunk.equals("--\r\n")) {
                continue;
            }
            String normalized = chunk.stripLeading();
            int headerSeparator = normalized.indexOf("\r\n\r\n");
            String headersBlock = normalized.substring(0, headerSeparator);
            String contentBlock = normalized.substring(headerSeparator + 4);
            if (contentBlock.endsWith("\r\n")) {
                contentBlock = contentBlock.substring(0, contentBlock.length() - 2);
            }
            String filename = headersBlock.lines()
                    .filter(line -> line.startsWith("Content-Disposition"))
                    .map(line -> line.substring(line.indexOf("filename=\"") + 10, line.lastIndexOf('"')))
                    .findFirst()
                    .orElseThrow();
            parts.put(filename, contentBlock.getBytes(StandardCharsets.ISO_8859_1));
        }
        return new MultipartParts(parts);
    }

    private ParsedData parseData(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        String magic = readAscii(buffer, "DB-VAK".length());
        int version = Short.toUnsignedInt(buffer.getShort());
        long recordCount = Integer.toUnsignedLong(buffer.getInt());
        List<ParsedDataRecord> records = new ArrayList<>();
        List<Integer> recordByteLengths = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            int recordStart = buffer.position();
            String threatName = readUtf8String(buffer);
            byte[] firstBytes = readByteArray(buffer);
            byte[] remainderHash = readByteArray(buffer);
            long remainderLength = buffer.getLong();
            String fileType = readUtf8String(buffer);
            long offsetStart = buffer.getLong();
            long offsetEnd = buffer.getLong();
            records.add(new ParsedDataRecord(threatName, firstBytes, remainderHash, remainderLength, fileType, offsetStart, offsetEnd));
            recordByteLengths.add(buffer.position() - recordStart);
        }
        return new ParsedData(magic, version, recordCount, records, recordByteLengths);
    }

    private ParsedManifest parseManifest(byte[] bytes, int expectedRecordCount) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        String magic = readAscii(buffer, "MF-VAK".length());
        int version = Short.toUnsignedInt(buffer.getShort());
        int exportType = Byte.toUnsignedInt(buffer.get());
        long generatedAtEpochMillis = buffer.getLong();
        long sinceEpochMillis = buffer.getLong();
        long recordCount = Integer.toUnsignedLong(buffer.getInt());
        byte[] dataSha256 = new byte[32];
        buffer.get(dataSha256);
        List<ParsedManifestEntry> entries = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            UUID id = new UUID(buffer.getLong(), buffer.getLong());
            int statusCode = Byte.toUnsignedInt(buffer.get());
            long updatedAtEpochMillis = buffer.getLong();
            long dataOffset = buffer.getLong();
            long dataLength = Integer.toUnsignedLong(buffer.getInt());
            int signatureLength = buffer.getInt();
            byte[] signatureBytes = new byte[signatureLength];
            buffer.get(signatureBytes);
            entries.add(new ParsedManifestEntry(id, statusCode, updatedAtEpochMillis, dataOffset, dataLength, signatureBytes));
        }
        int unsignedLength = buffer.position();
        int manifestSignatureLength = buffer.getInt();
        byte[] manifestSignatureBytes = new byte[manifestSignatureLength];
        buffer.get(manifestSignatureBytes);
        byte[] unsignedBytes = java.util.Arrays.copyOf(bytes, unsignedLength);
        assertThat(recordCount).isEqualTo(expectedRecordCount);
        assertThat(generatedAtEpochMillis).isPositive();
        return new ParsedManifest(
                magic,
                version,
                exportType,
                generatedAtEpochMillis,
                sinceEpochMillis,
                recordCount,
                dataSha256,
                entries,
                manifestSignatureBytes,
                unsignedBytes
        );
    }

    private String readAscii(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private String readUtf8String(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readByteArray(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    private byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return result;
    }

    private record MultipartParts(Map<String, byte[]> parts) {
    }

    private record ParsedData(
            String magic,
            int version,
            long recordCount,
            List<ParsedDataRecord> records,
            List<Integer> recordByteLengths
    ) {
    }

    private record ParsedDataRecord(
            String threatName,
            byte[] firstBytes,
            byte[] remainderHash,
            long remainderLength,
            String fileType,
            long offsetStart,
            long offsetEnd
    ) {
    }

    private record ParsedManifest(
            String magic,
            int version,
            int exportType,
            long generatedAtEpochMillis,
            long sinceEpochMillis,
            long recordCount,
            byte[] dataSha256,
            List<ParsedManifestEntry> entries,
            byte[] manifestSignatureBytes,
            byte[] unsignedBytes
    ) {
    }

    private record ParsedManifestEntry(
            UUID id,
            int statusCode,
            long updatedAtEpochMillis,
            long dataOffset,
            long dataLength,
            byte[] recordSignatureBytes
    ) {
    }
}
