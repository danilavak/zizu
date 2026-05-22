package ru.danilavak.zizu.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.signature")
public record SignatureModuleProperties(
        String keystoreLocation,
        String keystorePassword,
        String keyAlias,
        String keyPassword
) {
    public boolean isConfigured() {
        return keystoreLocation != null && !keystoreLocation.isBlank()
                && keystorePassword != null && !keystorePassword.isBlank()
                && keyAlias != null && !keyAlias.isBlank()
                && keyPassword != null && !keyPassword.isBlank();
    }
}
