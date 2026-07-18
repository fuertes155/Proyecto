package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.domain.identity.port.EncryptionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetPublicKeyUseCase {

    private final EncryptionPort encryptionPort;

    public Map<String, String> execute() {
        return Map.of("publicKey", encryptionPort.getRsaPublicKeyBase64());
    }
}
