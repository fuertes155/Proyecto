package com.cooperativa.met.infrastructure.persistence.identity;

import com.cooperativa.met.domain.compliance.model.AlertSeverity;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.AlertType;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.model.RegulatoryReport;
import com.cooperativa.met.domain.compliance.model.ReportStatus;
import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.domain.compliance.port.RegulatoryReportPort;
import com.cooperativa.met.domain.compliance.port.RestrictiveListRepositoryPort;
import com.cooperativa.met.domain.identity.model.BiometricRegistration;
import com.cooperativa.met.domain.identity.model.ComplianceCheckRecord;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.BiometricRegistrationPort;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adaptadores de identidad, cumplimiento y notificaciones contra H2. */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class IdentityCompliancePersistenceIntegrationTest {

    @Autowired private UserRepositoryPort userRepository;
    @Autowired private BiometricRegistrationPort biometricPort;
    @Autowired private ComplianceCheckPort complianceCheckPort;
    @Autowired private ComplianceAlertRepositoryPort alertRepository;
    @Autowired private RegulatoryReportPort reportPort;
    @Autowired private RestrictiveListRepositoryPort restrictiveListRepository;
    @Autowired private NotificationRepositoryPort notificationRepository;

    @Test
    void shouldFindUsersByTheirEncryptedDocumentAndByContactData() {
        String document = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        String phone = "300" + document.substring(0, 7);
        User saved = userRepository.save(user(document, document + "@example.com", phone));

        assertEquals(saved.getId(), userRepository.findByDocument(DocumentType.CC, document).orElseThrow().getId());
        assertEquals("Ana", userRepository.findById(saved.getId()).orElseThrow().getFirstName());
        assertEquals(saved.getId(), userRepository.findByEmail(document + "@example.com").orElseThrow().getId());
        assertEquals(saved.getId(), userRepository.findByPhone(phone).orElseThrow().getId());
        assertTrue(userRepository.existsByDocument(DocumentType.CC, document));
        assertFalse(userRepository.existsByDocument(DocumentType.CE, document));
        assertTrue(userRepository.existsByEmail(document + "@example.com"));
        assertTrue(userRepository.findAll().stream().anyMatch(u -> u.getId().equals(saved.getId())));
    }

    @Test
    void shouldReturnTheLatestBiometricRegistration() {
        UUID userId = UUID.randomUUID();
        biometricPort.save(biometric(userId, Instant.now().minus(Duration.ofDays(30))));
        BiometricRegistration latest = biometricPort.save(biometric(userId, Instant.now()));

        assertEquals(latest.getId(), biometricPort.findLatestByUserId(userId).orElseThrow().getId());
    }

    @Test
    void shouldKeepScreeningHistoryAndListMatches() {
        UUID userId = UUID.randomUUID();
        complianceCheckPort.persistCheck(userId, ComplianceListType.OFAC, ComplianceResult.MATCH, "{\"score\":0.91}");
        complianceCheckPort.persistCheck(userId, ComplianceListType.ONU, ComplianceResult.CLEAR, null);

        List<ComplianceCheckRecord> matches = complianceCheckPort.findByResult(ComplianceResult.MATCH, 0, 500);
        assertTrue(matches.stream().anyMatch(m -> m.getUserId().equals(userId) && m.getListType() == ComplianceListType.OFAC));
        assertTrue(matches.stream().noneMatch(m -> m.getUserId().equals(userId) && m.getListType() == ComplianceListType.ONU));
    }

    @Test
    void shouldQueueAlertsByStatusForTheComplianceOfficer() {
        UUID userId = UUID.randomUUID();
        ComplianceAlert open = alertRepository.save(alert(userId, AlertStatus.OPEN));
        alertRepository.save(alert(userId, AlertStatus.DISMISSED));
        long openBefore = alertRepository.countByStatus(AlertStatus.OPEN);

        alertRepository.save(open.review(UUID.randomUUID(), AlertStatus.REPORTED, "Reportado a la UIAF"));

        assertEquals(openBefore - 1, alertRepository.countByStatus(AlertStatus.OPEN));
        assertEquals(AlertStatus.REPORTED, alertRepository.findById(open.getId()).orElseThrow().getStatus());
        assertEquals(2, alertRepository.findByUserId(userId).size());
        assertTrue(alertRepository.findByStatus(AlertStatus.REPORTED, 0, 500).stream().anyMatch(a -> a.getId().equals(open.getId())));
    }

    @Test
    void shouldFindRegulatoryReportsByTypeAndPeriod() {
        int year = 2000 + ThreadLocalRandom.current().nextInt(50);
        RegulatoryReport saved = reportPort.save(RegulatoryReport.builder()
                .id(UUID.randomUUID()).reportType(SupersolidariaReportType.CREDITO_PERSONAL).periodYear(year).periodMonth(7)
                .entityCode("COOP001").status(ReportStatus.COMPLETED).fileName("x.txt").filePath("/tmp/x.txt")
                .fileSizeBytes(120L).recordCount(3).checksumSha256("abc").createdAt(Instant.now()).generatedAt(Instant.now()).build());

        assertEquals(saved.getId(), reportPort.findByTypeAndPeriod(SupersolidariaReportType.CREDITO_PERSONAL, year, 7).orElseThrow().getId());
        assertTrue(reportPort.findByTypeAndPeriod(SupersolidariaReportType.ASOCIADOS, year, 7).isEmpty());
        assertEquals(1, reportPort.findByPeriod(year, 7).size());
        assertEquals(3, reportPort.findById(saved.getId()).orElseThrow().getRecordCount());
        assertTrue(reportPort.findAll().stream().anyMatch(r -> r.getId().equals(saved.getId())));
    }

    @Test
    void shouldReplaceARestrictiveListCompletely() {
        restrictiveListRepository.deleteByListType(ComplianceListType.ONU);
        restrictiveListRepository.saveAll(List.of(entry("SANCIONADO UNO"), entry("SANCIONADO DOS")));
        assertEquals(2, restrictiveListRepository.countByListType(ComplianceListType.ONU));

        restrictiveListRepository.deleteByListType(ComplianceListType.ONU);

        assertEquals(0, restrictiveListRepository.countByListType(ComplianceListType.ONU));
    }

    @Test
    void shouldCountOnlyUnreadNotificationsOfTheUser() {
        UUID userId = UUID.randomUUID();
        Notification unread = notificationRepository.save(notification(userId, false, LocalDateTime.now()));
        notificationRepository.save(notification(userId, true, LocalDateTime.now().minusDays(1)));
        notificationRepository.save(notification(UUID.randomUUID(), false, LocalDateTime.now()));

        assertEquals(1, notificationRepository.countUnreadByUserId(userId));
        assertEquals(unread.getId(), notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).get(0).getId());
        assertEquals("Abono recibido", notificationRepository.findById(unread.getId()).orElseThrow().getTitle());
    }

    private User user(String document, String email, String phone) {
        Instant now = Instant.now();
        return User.builder()
                .id(UUID.randomUUID()).documentType(DocumentType.CC).documentNumber(document)
                .firstName("Ana").lastName("Pérez").email(email).phone(phone).pinHash("hash")
                .status(UserStatus.ACTIVE).kycStatus(KycStatus.APPROVED).termsAccepted(true).termsAcceptedAt(now)
                .createdAt(now).updatedAt(now).build();
    }

    private BiometricRegistration biometric(UUID userId, Instant createdAt) {
        return BiometricRegistration.builder()
                .id(UUID.randomUUID()).userId(userId).documentImage("enc-doc").documentBackImage("enc-back")
                .selfieImage("enc-selfie").signatureImage("enc-sign").livenessScore(new BigDecimal("0.9500"))
                .verified(true).verifiedAt(createdAt).createdAt(createdAt).build();
    }

    private ComplianceAlert alert(UUID userId, AlertStatus status) {
        return ComplianceAlert.builder()
                .id(UUID.randomUUID()).userId(userId).transactionId(UUID.randomUUID()).alertType(AlertType.UNUSUAL_AMOUNT)
                .severity(AlertSeverity.HIGH).description("Monto inusual").status(status).createdAt(Instant.now()).build();
    }

    private RestrictiveListEntry entry(String name) {
        return RestrictiveListEntry.builder()
                .id(UUID.randomUUID()).listType(ComplianceListType.ONU).fullName(name).normalizedName(name)
                .sourceRef("UN-" + name.hashCode()).sourceUpdatedAt(Instant.now()).createdAt(Instant.now()).build();
    }

    private Notification notification(UUID userId, boolean read, LocalDateTime createdAt) {
        return Notification.builder()
                .id(UUID.randomUUID()).userId(userId).title("Abono recibido").message("Recibiste $50.000")
                .read(read).createdAt(createdAt).build();
    }
}
