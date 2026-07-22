package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AesEncryptionAdapter implements EncryptionPort {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String AES_DETERMINISTIC_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int CBC_IV_LENGTH = 16;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";

    private final MetSecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // Instancia estática para acceso desde JPA Converters (que no son manejados por Spring DI fácilmente)
    public static EncryptionPort INSTANCE;

    private PrivateKey rsaPrivateKey;
    private String rsaPublicKeyBase64;

    @PostConstruct
    public void init() {
        INSTANCE = this;
        try {
            String b64Priv = securityProperties.getEncryption().getRsaPrivateKey();
            String b64Pub = securityProperties.getEncryption().getRsaPublicKey();

            if (b64Priv != null && !b64Priv.isBlank() && b64Pub != null && !b64Pub.isBlank()) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.rsaPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64Priv)));
                this.rsaPublicKeyBase64 = b64Pub;
            } else {
                // Generate on the fly for development
                KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
                keyPairGen.initialize(2048);
                KeyPair keyPair = keyPairGen.generateKeyPair();
                this.rsaPrivateKey = keyPair.getPrivate();
                this.rsaPublicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RSA keys", e);
        }
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Error al cifrar datos sensibles", ex);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Error al descifrar datos sensibles", ex);
        }
    }

    @Override
    public String encryptDeterministic(String plainText) {
        try {
            if (plainText == null) return null;
            // Generar IV determinista basado en el hash del texto plano (16 bytes para CBC)
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] iv = md.digest(plainText.getBytes(StandardCharsets.UTF_8));
            
            Cipher cipher = Cipher.getInstance(AES_DETERMINISTIC_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new javax.crypto.spec.IvParameterSpec(iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Error al cifrar datos buscables", ex);
        }
    }

    @Override
    public String decryptDeterministic(String cipherText) {
        try {
            if (cipherText == null) return null;
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[CBC_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(AES_DETERMINISTIC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new javax.crypto.spec.IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Error al descifrar datos buscables", ex);
        }
    }

    @Override
    public String hashPin(String pin) {
        return passwordEncoder.encode(pin);
    }

    @Override
    public boolean verifyPin(String pin, String hash) {
        return passwordEncoder.matches(pin, hash);
    }

    @Override
    public String hashBiometric(String biometricPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(biometricPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Error al hashear biometría", ex);
        }
    }

    @Override
    public String decryptRsa(String encryptedBase64) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Error descifrando payload RSA. Revisa las llaves o el padding.", ex);
        }
    }

    @Override
    public String getRsaPublicKeyBase64() {
        return rsaPublicKeyBase64;
    }

    private SecretKeySpec secretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(securityProperties.getEncryption().getAesKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
