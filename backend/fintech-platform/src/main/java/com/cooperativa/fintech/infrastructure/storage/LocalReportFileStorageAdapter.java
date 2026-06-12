package com.cooperativa.fintech.infrastructure.storage;

import com.cooperativa.fintech.domain.compliance.port.ReportFileStoragePort;
import com.cooperativa.fintech.infrastructure.config.FintechComplianceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class LocalReportFileStorageAdapter implements ReportFileStoragePort {

    private final FintechComplianceProperties complianceProperties;

    @Override
    public String store(String fileName, byte[] content) {
        try {
            Path dir = Paths.get(complianceProperties.getStoragePath());
            Files.createDirectories(dir);
            Path filePath = dir.resolve(fileName);
            Files.write(filePath, content);
            return filePath.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Error almacenando archivo regulatorio", ex);
        }
    }

    @Override
    public byte[] read(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException ex) {
            throw new IllegalStateException("Error leyendo archivo regulatorio", ex);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ex) {
            throw new IllegalStateException("Error eliminando archivo regulatorio", ex);
        }
    }
}
