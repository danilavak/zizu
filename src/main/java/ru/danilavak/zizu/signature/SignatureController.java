package ru.danilavak.zizu.signature;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/signature")
public class SignatureController {
    private final DigitalSignatureService digitalSignatureService;

    public SignatureController(DigitalSignatureService digitalSignatureService) {
        this.digitalSignatureService = digitalSignatureService;
    }

    @GetMapping("/certificate")
    public CertificateResponse certificate() {
        return new CertificateResponse(digitalSignatureService.exportCertificatePem());
    }

    public record CertificateResponse(String certificatePem) {
    }
}
