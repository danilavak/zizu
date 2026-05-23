package ru.danilavak.zizu.updates;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
public class UpdateManifestController {
    private final UpdateManifestService updateManifestService;

    public UpdateManifestController(UpdateManifestService updateManifestService) {
        this.updateManifestService = updateManifestService;
    }

    @GetMapping("/manifest")
    public UpdateManifestService.ManifestResponse manifest(
            @RequestParam(value = "since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since
    ) {
        return updateManifestService.buildManifest(since);
    }
}
