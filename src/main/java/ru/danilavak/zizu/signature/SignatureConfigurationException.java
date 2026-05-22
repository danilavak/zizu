package ru.danilavak.zizu.signature;

public class SignatureConfigurationException extends RuntimeException {
    public SignatureConfigurationException(String message) {
        super(message);
    }

    public SignatureConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
