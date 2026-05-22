package ru.danilavak.zizu.common.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ApiViolation> violations
) {
}
