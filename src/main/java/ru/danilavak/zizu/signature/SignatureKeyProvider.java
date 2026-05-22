package ru.danilavak.zizu.signature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class SignatureKeyProvider {
    private final SignatureModuleProperties properties;
    private final ResourceLoader resourceLoader;
    private volatile SignatureKeyMaterial cachedMaterial;

    public SignatureKeyProvider(SignatureModuleProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public SignatureKeyMaterial getKeyMaterial() {
        SignatureKeyMaterial current = cachedMaterial;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedMaterial == null) {
                cachedMaterial = loadMaterial();
            }
            return cachedMaterial;
        }
    }

    private SignatureKeyMaterial loadMaterial() {
        if (!properties.isConfigured()) {
            throw new SignatureConfigurationException("Signature module is not configured. Set keystore source, passwords and key alias.");
        }

        try (InputStream inputStream = openKeyStoreStream()) {
            KeyStore keyStore = KeyStore.getInstance(properties.resolvedKeystoreType());
            keyStore.load(inputStream, properties.keystorePassword().toCharArray());

            Key key = keyStore.getKey(properties.keyAlias(), properties.resolvedKeyPassword().toCharArray());
            if (!(key instanceof PrivateKey privateKey)) {
                throw new SignatureConfigurationException("Alias '%s' does not contain a private key".formatted(properties.keyAlias()));
            }

            Certificate certificate = keyStore.getCertificate(properties.keyAlias());
            if (!(certificate instanceof X509Certificate x509Certificate)) {
                throw new SignatureConfigurationException("Alias '%s' does not contain an X509 certificate".formatted(properties.keyAlias()));
            }

            PublicKey publicKey = x509Certificate.getPublicKey();
            return new SignatureKeyMaterial(privateKey, publicKey, x509Certificate);
        } catch (SignatureConfigurationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SignatureConfigurationException("Failed to load signature keystore", exception);
        }
    }

    private InputStream openKeyStoreStream() throws IOException {
        if (properties.keystoreBase64() != null && !properties.keystoreBase64().isBlank()) {
            byte[] bytes = Base64.getDecoder().decode(properties.keystoreBase64());
            return new ByteArrayInputStream(bytes);
        }

        Resource resource = resourceLoader.getResource(properties.keystoreLocation());
        if (resource.exists()) {
            return resource.getInputStream();
        }
        return resourceLoader.getResource("file:" + properties.keystoreLocation()).getInputStream();
    }
}
