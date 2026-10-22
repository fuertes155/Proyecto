package com.cooperativa.met.infrastructure.persistence.compliance;

import com.cooperativa.met.domain.compliance.model.ReportDataRow;
import com.cooperativa.met.domain.compliance.model.ReportDataset;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.met.domain.compliance.port.ReportDatasetPort;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Datos de los reportes para la Supersolidaria leídos con SQL directo sobre H2. La cédula y los
 * nombres se guardan cifrados en la tabla users (convertidores JPA), así que el adaptador tiene que
 * descifrarlos: el supervisor necesita los datos reales, no el texto cifrado.
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class ReportDatasetIntegrationTest {

    @Autowired private ReportDatasetPort datasetPort;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private ComplianceCheckPort complianceCheckPort;

    @Test
    void shouldReportAssociatesWithTheirRealDocumentAndName() {
        String document = randomDocument();
        saveUser(document, "Ana María", "Pérez Gómez", at(2026, 3, 10));

        ReportDataset dataset = datasetPort.buildDataset(SupersolidariaReportType.ASOCIADOS, 2026, 3, "COOP001");

        List<String> row = rowFor(dataset, document).orElseThrow(() ->
                new AssertionError("el asociado no aparece con su cédula real (¿quedó cifrada?)"));
        assertEquals("CC", row.get(1));
        assertEquals("Ana María", row.get(3));
        assertEquals("Pérez Gómez", row.get(4));
        assertEquals("20260310", row.get(8));
    }

    @Test
    void shouldIncludeEveryAssociateLinkedUpToTheEndOfThePeriod() {
        String linkedLastNovember = randomDocument();
        String linkedInApril = randomDocument();
        saveUser(linkedLastNovember, "Luis", "Rojas", at(2025, 11, 20));
        saveUser(linkedInApril, "Sara", "Díaz", at(2026, 4, 2));

        ReportDataset march = datasetPort.buildDataset(SupersolidariaReportType.ASOCIADOS, 2026, 3, "COOP001");

        assertTrue(rowFor(march, linkedLastNovember).isPresent(),
                "un asociado de noviembre del año anterior sigue siendo asociado en marzo");
        assertTrue(rowFor(march, linkedInApril).isEmpty(), "quien se vinculó después del período no va");
    }

    @Test
    void shouldReportSarlaftScreeningsWithTheRealDocument() {
        String document = randomDocument();
        User user = saveUser(document, "Carlos", "Mejía", at(2026, 1, 5));
        complianceCheckPort.persistCheck(user.getId(), ComplianceListType.OFAC, ComplianceResult.CLEAR, null);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        ReportDataset dataset = datasetPort.buildDataset(SupersolidariaReportType.SARLAFT,
                today.getYear(), today.getMonthValue(), "COOP001");

        List<String> row = rowFor(dataset, document).orElseThrow(() ->
                new AssertionError("el screening no aparece con la cédula real"));
        assertEquals("OFAC", row.get(2));
        assertEquals("CLEAR", row.get(3));
    }

    private User saveUser(String document, String firstName, String lastName, Instant createdAt) {
        return userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .documentType(DocumentType.CC)
                .documentNumber(document)
                .firstName(firstName)
                .lastName(lastName)
                .email(document + "@example.com")
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .termsAccepted(true)
                .termsAcceptedAt(createdAt)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());
    }

    private static Optional<List<String>> rowFor(ReportDataset dataset, String document) {
        return dataset.getDetailRows().stream()
                .map(ReportDataRow::getFields)
                .filter(fields -> fields.contains(document))
                .findFirst();
    }

    private static Instant at(int year, int month, int day) {
        return LocalDate.of(year, month, day).atTime(12, 0).toInstant(ZoneOffset.UTC);
    }

    private static String randomDocument() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
    }
}
