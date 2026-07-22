package com.cooperativa.met.infrastructure.security.converter;

import com.cooperativa.met.infrastructure.security.AesEncryptionAdapter;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StandardCryptoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (AesEncryptionAdapter.INSTANCE == null) {
            throw new IllegalStateException("EncryptionPort (AesEncryptionAdapter) no esta inicializado");
        }
        return AesEncryptionAdapter.INSTANCE.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (AesEncryptionAdapter.INSTANCE == null) {
            throw new IllegalStateException("EncryptionPort (AesEncryptionAdapter) no esta inicializado");
        }
        return AesEncryptionAdapter.INSTANCE.decrypt(dbData);
    }
}
