package ru.danilavak.zizu.binaryapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.binary-api")
public record BinaryApiProperties(
        String studentSurname,
        int version
) {
    public String normalizedSurname() {
        if (studentSurname == null || studentSurname.isBlank()) {
            return "VAK";
        }
        return studentSurname.trim().toUpperCase();
    }

    public int resolvedVersion() {
        return version <= 0 ? 1 : version;
    }
}
