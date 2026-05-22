package ru.danilavak.zizu.binaryapi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MultiValueMap;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/binary/signatures")
public class BinarySignatureExportController {
    private final BinarySignatureExportService exportService;
    private final MultipartMixedResponseFactory responseFactory;

    public BinarySignatureExportController(
            BinarySignatureExportService exportService,
            MultipartMixedResponseFactory responseFactory
    ) {
        this.exportService = exportService;
        this.responseFactory = responseFactory;
    }

    @GetMapping(value = "/full", produces = MediaType.MULTIPART_MIXED_VALUE)
    public ResponseEntity<MultiValueMap<String, HttpEntity<ByteArrayResource>>> full() {
        return multipartResponse(exportService.exportFull());
    }

    @GetMapping(value = "/increment", produces = MediaType.MULTIPART_MIXED_VALUE)
    public ResponseEntity<MultiValueMap<String, HttpEntity<ByteArrayResource>>> increment(
            @RequestParam("since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since
    ) {
        return multipartResponse(exportService.exportIncrement(since));
    }

    @PostMapping(value = "/by-ids", produces = MediaType.MULTIPART_MIXED_VALUE)
    public ResponseEntity<MultiValueMap<String, HttpEntity<ByteArrayResource>>> byIds(
            @Valid @RequestBody ByIdsRequest request
    ) {
        return multipartResponse(exportService.exportByIds(request.ids()));
    }

    private ResponseEntity<MultiValueMap<String, HttpEntity<ByteArrayResource>>> multipartResponse(
            BinarySignatureExportService.BinaryExportPayload payload
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.MULTIPART_MIXED)
                .body(responseFactory.create(payload.parts()));
    }

    public record ByIdsRequest(@NotEmpty List<@NotNull UUID> ids) {
    }
}
