package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesEncryptionAdapterTest {

    private static final String AES_KEY = "test-aes-key-32-characters-long!!";

    private AesEncryptionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = newAdapter(AES_KEY, null, null);
    }

    @Test
    void shouldRoundTripSensitiveDataWithAFreshIvEachTime() {
        String first = adapter.encrypt("1086578123");
        String second = adapter.encrypt("1086578123");

        assertNotEquals(first, second, "el mismo dato no debe cifrar igual dos veces");
        assertFalse(first.contains("1086578123"));
        assertEquals("1086578123", adapter.decrypt(first));
        assertEquals("1086578123", adapter.decrypt(second));
    }

    @Test
    void shouldRejectTamperedCiphertext() {
        byte[] bytes = Base64.getDecoder().decode(adapter.encrypt("saldo:1500000"));
        bytes[bytes.length - 1] ^= 0x01;

        assertThrows(IllegalStateException.class, () -> adapter.decrypt(Base64.getEncoder().encodeToString(bytes)));
    }

    @Test
    void shouldNotDecryptWithAnotherKey() {
        String cipherText = adapter.encrypt("dato");
        AesEncryptionAdapter otherKey = newAdapter("otra-llave-de-32-caracteres-xxxxx", null, null);

        assertThrows(IllegalStateException.class, () -> otherKey.decrypt(cipherText));
    }

    @Test
    void shouldEncryptSearchableValuesAlwaysTheSameWay() {
        String first = adapter.encryptDeterministic("socio@example.com");

        assertEquals(first, adapter.encryptDeterministic("socio@example.com"), "debe poder buscarse por igualdad en la BD");
        assertNotEquals(first, adapter.encryptDeterministic("otro@example.com"));
        assertEquals("socio@example.com", adapter.decryptDeterministic(first));
    }

    @Test
    void shouldKeepNullsAsNullInSearchableEncryption() {
        assertNull(adapter.encryptDeterministic(null));
        assertNull(adapter.decryptDeterministic(null));
    }

    @Test
    void shouldRejectTamperedSearchableCiphertextBeforeDecrypting() {
        byte[] bytes = Base64.getDecoder().decode(adapter.encryptDeterministic("1086578123"));
        bytes[bytes.length - 1] ^= 0x01;

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.decryptDeterministic(Base64.getEncoder().encodeToString(bytes)));

        // El HMAC se revisa antes de descifrar: el rechazo es por integridad, nunca por padding.
        assertInstanceOf(SecurityException.class, ex.getCause());
    }

    @Test
    void shouldHashPinWithSaltAndVerifyIt() {
        String hash = adapter.hashPin("4821");

        assertNotEquals("4821", hash);
        assertNotEquals(hash, adapter.hashPin("4821"), "cada hash lleva su propia sal");
        assertTrue(adapter.verifyPin("4821", hash));
        assertFalse(adapter.verifyPin("4822", hash));
    }

    @Test
    void shouldHashBiometricPayloadWithSha256() throws Exception {
        String expected = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest("template".getBytes(StandardCharsets.UTF_8)));

        assertEquals(expected, adapter.hashBiometric("template"));
    }

    @Test
    void shouldGenerateRsaKeysForDevelopmentAndDecryptWhatTheAppEncrypts() throws Exception {
        String encryptedPin = rsaEncrypt(publicKey(adapter.getRsaPublicKeyBase64()), "4821");

        assertEquals("4821", adapter.decryptRsa(encryptedPin));
        assertSame(adapter, AesEncryptionAdapter.INSTANCE);
    }

    @Test
    void shouldUseConfiguredRsaKeysEvenWhenPastedWithLineBreaks() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        String publicB64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateB64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        AesEncryptionAdapter configured = newAdapter(AES_KEY, withLineBreaks(privateB64), withLineBreaks(publicB64));

        assertEquals(publicB64, configured.getRsaPublicKeyBase64());
        assertEquals("4821", configured.decryptRsa(rsaEncrypt(keyPair.getPublic(), "4821")));
    }

    @Test
    void shouldFailFastOnUnusableRsaPrivateKey() {
        assertThrows(IllegalStateException.class, () -> newAdapter(AES_KEY, "bm8tZXMtdW5hLWxsYXZl", "bm8tZXMtdW5hLWxsYXZl"));
    }

    @Test
    void shouldRejectRsaPayloadEncryptedWithAnotherKey() throws Exception {
        String foreign = rsaEncrypt(rsaKeyPair().getPublic(), "4821");

        assertThrows(IllegalArgumentException.class, () -> adapter.decryptRsa(foreign));
    }

    private static AesEncryptionAdapter newAdapter(String aesKey, String rsaPrivate, String rsaPublic) {
        MetSecurityProperties properties = new MetSecurityProperties();
        properties.getEncryption().setAesKey(aesKey);
        properties.getEncryption().setRsaPrivateKey(rsaPrivate);
        properties.getEncryption().setRsaPublicKey(rsaPublic);
        AesEncryptionAdapter adapter = new AesEncryptionAdapter(properties);
        adapter.init();
        return adapter;
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static PublicKey publicKey(String base64) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    private static String rsaEncrypt(PublicKey key, String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    private static String withLineBreaks(String base64) {
        return String.join("\n", base64.split("(?<=\\G.{64})")) + "\n";
    }
}
