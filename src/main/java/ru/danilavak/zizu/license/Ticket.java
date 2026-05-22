package ru.danilavak.zizu.license;

import java.time.Instant;
import java.util.UUID;

public record Ticket(
        Instant serverDate,
        long lifetimeSeconds,
        Instant licenseActivationDate,
        Instant licenseExpirationDate,
        Long userId,
        UUID deviceId,
        boolean blocked
) {
}
