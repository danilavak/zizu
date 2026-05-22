package ru.danilavak.zizu.signature;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class DigitalSignatureService {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final SignatureKeyProvider keyProvider;
    private final JsonCanonicalizationService canonicalizationService;

    public DigitalSignatureService(SignatureKeyProvider keyProvider, JsonCanonicalizationService canonicalizationService) {
        this.keyProvider = keyProvider;
        this.canonicalizationService = canonicalizationService;
    }

    public String signObject(Object payload) {
        return signBytes(canonicalizationService.canonicalize(payload));
    }

    public boolean verifyObject(Object payload, String signatureBase64) {
        return verifyBytes(canonicalizationService.canonicalize(payload), signatureBase64);
    }

    public String signBytes(byte[] payload) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(keyProvider.getKeyMaterial().privateKey());
            signature.update(payload);
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new SignatureConfigurationException("Failed to sign payload", exception);
        }
    }

    public boolean verifyBytes(byte[] payload, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(keyProvider.getKeyMaterial().publicKey());
            signature.update(payload);
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String exportCertificatePem() {
        try {
            String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(keyProvider.getKeyMaterial().certificate().getEncoded());
            return "-----BEGIN CERTIFICATE-----\n" + encoded + "\n-----END CERTIFICATE-----\n";
        } catch (GeneralSecurityException exception) {
            throw new SignatureConfigurationException("Failed to export public certificate", exception);
        }
    }
}
