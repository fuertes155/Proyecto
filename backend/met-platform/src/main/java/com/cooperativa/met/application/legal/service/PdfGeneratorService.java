package com.cooperativa.met.application.legal.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfGeneratorService {

    public PdfResult generateMandatePdf(String userName, String documentNumber, String ipAddress, Instant signedAt) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 12, Font.NORMAL);

            document.add(new Paragraph("CONTRATO DE MANDATO Y TÉRMINOS DE USO", titleFont));
            document.add(new Paragraph("\n"));

            String dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                    .withZone(ZoneId.of("America/Bogota"))
                    .format(signedAt);

            String text = String.format("Yo, %s, identificado(a) con documento número %s, " +
                    "acepto y otorgo mandato especial a MET Cooperativa para la administración y " +
                    "dispersión de mis fondos en calidad de ahorro e inversión, conforme a las " +
                    "políticas y términos de uso vigentes.\n\n" +
                    "Firma digital realizada mediante validación OTP.\n\n" +
                    "Metadatos Legales:\n" +
                    "- Fecha y Hora: %s\n" +
                    "- Dirección IP: %s\n", userName, documentNumber, dateStr, ipAddress);

            document.add(new Paragraph(text, bodyFont));
            document.close();

            byte[] pdfBytes = out.toByteArray();
            String hashSha256 = calculateHash(pdfBytes);

            log.info("PDF Mandato generado exitosamente. Hash: {}", hashSha256);
            return new PdfResult(pdfBytes, hashSha256);

        } catch (Exception e) {
            log.error("Error generando PDF del mandato", e);
            throw new RuntimeException("Error generando PDF", e);
        }
    }

    private String calculateHash(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(content);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public record PdfResult(byte[] pdfContent, String hashSha256) {}
}
