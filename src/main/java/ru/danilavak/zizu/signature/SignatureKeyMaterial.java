package ru.danilavak.zizu.signature;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

record SignatureKeyMaterial(
        PrivateKey privateKey,
        PublicKey publicKey,
        X509Certificate certificate
) {
}
