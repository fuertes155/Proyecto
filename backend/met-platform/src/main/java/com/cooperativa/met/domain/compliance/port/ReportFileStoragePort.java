package com.cooperativa.met.domain.compliance.port;

public interface ReportFileStoragePort {

    String store(String fileName, byte[] content);

    byte[] read(String filePath);

    void delete(String filePath);
}
