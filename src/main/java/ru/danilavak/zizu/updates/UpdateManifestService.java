package ru.danilavak.zizu.updates;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.danilavak.zizu.malware.MalwareSignatureFile;
import ru.danilavak.zizu.malware.MalwareSignatureFileRepository;
import ru.danilavak.zizu.malware.MalwareSignatureService;
import ru.danilavak.zizu.signature.DigitalSignatureService;
import ru.danilavak.zizu.storage.ObjectStorageService;

@Service
public class UpdateManifestService {
    private final MalwareSignatureService malwareSignatureService;
    private final MalwareSignatureFileRepository malwareSignatureFileRepository;
    private final ObjectStorageService objectStorageService;
    private final DigitalSignatureService digitalSignatureService;

    public UpdateManifestService(
            MalwareSignatureService malwareSignatureService,
            MalwareSignatureFileRepository malwareSignatureFileRepository,
            ObjectStorageService objectStorageService,
            DigitalSignatureService digitalSignatureService
    ) {
        this.malwareSignatureService = malwareSignatureService;
        this.malwareSignatureFileRepository = malwareSignatureFileRepository;
        this.objectStorageService = objectStorageService;
        this.digitalSignatureService = digitalSignatureService;
    }

    @Transactional(readOnly = true)
    public ManifestResponse buildManifest(Instant since) {
        List<MalwareSignatureService.SignatureResponse> signatures = since == null
                ? malwareSignatureService.getFullDatabase()
                : malwareSignatureService.getIncrement(since);

        Map<UUID, MalwareSignatureFile> filesById = new LinkedHashMap<>();
        for (MalwareSignatureFile file : malwareSignatureFileRepository.findAllBySignatureIdIn(
                signatures.stream().map(MalwareSignatureService.SignatureResponse::id).toList())) {
            filesById.put(file.getSignatureId(), file);
        }

        List<ManifestEntry> entries = signatures.stream()
                .map(signature -> toManifestEntry(signature, filesById.get(signature.id())))
                .toList();

        UnsignedManifest unsignedManifest = new UnsignedManifest(
                Instant.now(),
                since,
                entries.stream().map(ManifestEntry::updatedAt).max(Instant::compareTo).orElse(Instant.EPOCH),
                entries
        );

        return new ManifestResponse(
                unsignedManifest.issuedAt(),
                unsignedManifest.since(),
                unsignedManifest.databaseVersion(),
                unsignedManifest.entries(),
                digitalSignatureService.signObject(unsignedManifest)
        );
    }

    private ManifestEntry toManifestEntry(
            MalwareSignatureService.SignatureResponse signature,
            MalwareSignatureFile file
    ) {
        URI presignedUrl = file == null ? null : objectStorageService.createPresignedGetUrl(file.getObjectKey());
        return new ManifestEntry(
                signature.id(),
                signature.updatedAt(),
                signature.status().name(),
                signature.threatName(),
                signature.fileType(),
                file == null ? null : file.getOriginalFilename(),
                file == null ? null : file.getSizeBytes(),
                file == null ? null : presignedUrl,
                signature.digitalSignatureBase64()
        );
    }

    private record UnsignedManifest(
            Instant issuedAt,
            Instant since,
            Instant databaseVersion,
            List<ManifestEntry> entries
    ) {
    }

    public record ManifestResponse(
            Instant issuedAt,
            Instant since,
            Instant databaseVersion,
            List<ManifestEntry> entries,
            String manifestSignatureBase64
    ) {
    }

    public record ManifestEntry(
            UUID signatureId,
            Instant updatedAt,
            String status,
            String threatName,
            String fileType,
            String fileName,
            Long fileSizeBytes,
            URI downloadUrl,
            String recordSignatureBase64
    ) {
    }
}
