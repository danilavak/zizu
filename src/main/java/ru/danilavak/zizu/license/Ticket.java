package ru.danilavak.zizu.license;

import java.time.Instant;

public record Ticket(
        Instant serverDate,
        long lifetimeSeconds,
        Instant licenseActivationDate,
        Instant licenseExpirationDate,
        Long userId,
        Long deviceId,
        boolean blocked
) {
}
