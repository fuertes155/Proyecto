package com.cooperativa.met.infrastructure.persistence.legal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mandate_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandateContractJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    // Sin @Lob a propósito: en PostgreSQL, @Lob sobre un byte[] hace que Hibernate
    // intente usar Large Objects (OID, tipo bigint) en vez de escribir directo en
    // la columna bytea, lo que revienta con "column pdf_content is of type bytea
    // but expression is of type bigint". Un byte[] plano sí mapea correctamente.
    @Column(name = "pdf_content", nullable = false)
    private byte[] pdfContent;

    @Column(name = "pdf_hash_sha256", nullable = false)
    private String pdfHashSha256;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false)
    private String userAgent;

    @Column(name = "otp_transaction_id", nullable = false)
    private String otpTransactionId;

    @Column(nullable = false)
    private String status;
}
