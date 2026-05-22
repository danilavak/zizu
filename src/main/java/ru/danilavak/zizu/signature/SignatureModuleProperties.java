package ru.danilavak.zizu.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.signature")
public record SignatureModuleProperties(
        String keystoreBase64,
        String keystoreLocation,
        String keystoreType,
        String keystorePassword,
        String keyAlias,
        String keyPassword
) {
    public boolean isConfigured() {
        boolean hasKeystoreSource = (keystoreLocation != null && !keystoreLocation.isBlank())
                || (keystoreBase64 != null && !keystoreBase64.isBlank());
        return hasKeystoreSource
                && keystorePassword != null && !keystorePassword.isBlank()
                && keyAlias != null && !keyAlias.isBlank();
    }

    public String resolvedKeyPassword() {
        return keyPassword == null || keyPassword.isBlank() ? keystorePassword : keyPassword;
    }

    public String resolvedKeystoreType() {
        return keystoreType == null || keystoreType.isBlank() ? "PKCS12" : keystoreType.trim();
    }
}
