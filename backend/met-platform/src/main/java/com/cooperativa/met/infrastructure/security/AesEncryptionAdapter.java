package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AesEncryptionAdapter implements EncryptionPort {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    // CBC en vez de GCM para poder derivar el IV del propio texto plano (ver deterministicIv):
    // así el mismo valor cifra siempre igual y se puede buscar por igualdad en la BD.
    // La falta de autenticación nativa de CBC se compensa con un HMAC Encrypt-then-MAC
    // (deterministicMacTag) que se verifica ANTES de descifrar, cerrando el padding oracle.
    private static final String AES_DETERMINISTIC_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int CBC_IV_LENGTH = 16;
    private static final int HMAC_TAG_LENGTH = 32;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MetSecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Instancia estática para acceso desde JPA Converters (que no son manejados por Spring DI fácilmente).
    public static EncryptionPort INSTANCE;

    private PrivateKey rsaPrivateKey;
    private String rsaPublicKeyBase64;

    @PostConstruct
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification = "Bridge singleton para JPA AttributeConverters, fuera del alcance de Spring DI; init() es un @PostConstruct de un bean singleton, corre una sola vez en el arranque")
    public void init() {
        INSTANCE = this;
        try {
            // Las variables de entorno suelen llegar con saltos de línea o espacios
            // (p. ej. al pegar la clave en un textarea del panel del hosting). El
            // decodificador estricto de Base64 revienta con "incorrect ending byte",
            // así que se limpian los espacios en blanco antes de decodificar.
            String b64Priv = stripWhitespace(securityProperties.getEncryption().getRsaPrivateKey());
            String b64Pub = stripWhitespace(securityProperties.getEncryption().getRsaPublicKey());

            if (b64Priv != null && !b64Priv.isBlank() && b64Pub != null && !b64Pub.isBlank()) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.rsaPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(b64Priv)));
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

    private static String stripWhitespace(String value) {
        return value == null ? null : value.replaceAll("\\s", "");
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

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
    @SuppressFBWarnings(value = {"CIPHER_INTEGRITY", "PADDING_ORACLE"},
            justification = "CBC es necesario para derivar un IV determinista del texto plano (cifrado buscable por igualdad). "
                    + "La integridad la da el HMAC Encrypt-then-MAC (deterministicMacTag), verificado en decryptDeterministic "
                    + "ANTES de invocar Cipher.doFinal, por lo que un texto cifrado manipulado nunca llega a la etapa de padding.")
    public String encryptDeterministic(String plainText) {
        try {
            if (plainText == null) return null;
            byte[] iv = deterministicIv(plainText);

            Cipher cipher = Cipher.getInstance(AES_DETERMINISTIC_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] tag = deterministicMacTag(iv, cipherBytes);

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + tag.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(tag);
            buffer.put(cipherBytes);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Error al cifrar datos buscables", ex);
        }
    }

    @Override
    @SuppressFBWarnings(value = {"CIPHER_INTEGRITY", "PADDING_ORACLE"},
            justification = "El HMAC se verifica en tiempo constante antes de llamar a Cipher.doFinal, por lo que un ciphertext "
                    + "alterado se rechaza por integridad y nunca expone una excepción de padding a quien llama.")
    public String decryptDeterministic(String cipherText) {
        try {
            if (cipherText == null) return null;
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[CBC_IV_LENGTH];
            buffer.get(iv);
            byte[] tag = new byte[HMAC_TAG_LENGTH];
            buffer.get(tag);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            byte[] expectedTag = deterministicMacTag(iv, cipherBytes);
            if (!MessageDigest.isEqual(tag, expectedTag)) {
                throw new SecurityException("Integridad del dato cifrado comprometida");
            }

            Cipher cipher = Cipher.getInstance(AES_DETERMINISTIC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new IvParameterSpec(iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
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

    // IV determinista (16 bytes) para el cifrado buscable: HMAC keyed en vez de MD5(texto plano) sin llave,
    // así el IV no es predecible por un atacante que solo conozca el texto plano.
    private byte[] deterministicIv(String plainText) throws Exception {
        byte[] full = hmac(deterministicIvKey(), plainText.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(full, CBC_IV_LENGTH);
    }

    // Tag HMAC-SHA256 sobre (iv || ciphertext): provee integridad tipo Encrypt-then-MAC para el modo CBC.
    private byte[] deterministicMacTag(byte[] iv, byte[] cipherBytes) throws Exception {
        byte[] ivAndCipher = ByteBuffer.allocate(iv.length + cipherBytes.length).put(iv).put(cipherBytes).array();
        return hmac(deterministicMacKey(), ivAndCipher);
    }

    private byte[] hmac(SecretKeySpec key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(key);
        return mac.doFinal(data);
    }

    private SecretKeySpec deterministicIvKey() throws Exception {
        return derivedHmacKey("IV");
    }

    private SecretKeySpec deterministicMacKey() throws Exception {
        return derivedHmacKey("MAC");
    }

    // Deriva llaves HMAC independientes para el IV y para el tag de integridad a partir de la misma
    // llave AES base, con etiquetas distintas para evitar reutilizar exactamente la misma llave en dos usos.
    private SecretKeySpec derivedHmacKey(String label) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest((securityProperties.getEncryption().getAesKey() + ":" + label).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, HMAC_ALGORITHM);
    }
}
