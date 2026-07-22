package com.cooperativa.met.domain.identity.port;

public interface EncryptionPort {

    String encrypt(String plainText);
    String decrypt(String cipherText);

    // Deterministic encryption for searchable fields
    String encryptDeterministic(String plainText);
    String decryptDeterministic(String cipherText);

    String hashPin(String pin);

    boolean verifyPin(String pin, String hash);

    String hashBiometric(String biometricPayload);

    // RSA E2EE
    String decryptRsa(String encryptedBase64);
    String getRsaPublicKeyBase64();
}
