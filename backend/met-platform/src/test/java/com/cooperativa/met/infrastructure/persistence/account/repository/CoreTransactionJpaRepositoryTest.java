package com.cooperativa.met.infrastructure.persistence.account.repository;

import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.infrastructure.persistence.account.entity.CoreTransactionJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test") // Para usar la base de datos en memoria H2
class CoreTransactionJpaRepositoryTest {

    @Autowired
    private CoreTransactionJpaRepository repository;

    @Test
    void shouldFindTransactionsOnlyForSpecificAccount() {
        // Arrange: Crear cuentas de prueba
        UUID accountA = UUID.randomUUID();
        UUID accountB = UUID.randomUUID();
        UUID accountC = UUID.randomUUID();

        // Transacción 1: Cuenta A envía a Cuenta B
        CoreTransactionJpaEntity tx1 = CoreTransactionJpaEntity.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(accountA)
                .destinationAccountId(accountB)
                .amount(new BigDecimal("100.00"))
                .concept("Pago 1")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(100))
                .build();

        // Transacción 2: Cuenta C envía a Cuenta A
        CoreTransactionJpaEntity tx2 = CoreTransactionJpaEntity.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(accountC)
                .destinationAccountId(accountA)
                .amount(new BigDecimal("200.00"))
                .concept("Pago 2")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(50))
                .build();

        // Transacción 3: Cuenta B envía a Cuenta C (NO involucra a la Cuenta A)
        CoreTransactionJpaEntity tx3 = CoreTransactionJpaEntity.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(accountB)
                .destinationAccountId(accountC)
                .amount(new BigDecimal("300.00"))
                .concept("Secreto")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        repository.saveAll(List.of(tx1, tx2, tx3));

        // Act: Buscar el historial de la Cuenta A
        List<CoreTransactionJpaEntity> result = repository.findByAccountId(accountA);

        // Assert:
        // 1. Debe retornar exactamente 2 transacciones (tx1 y tx2)
        assertEquals(2, result.size());

        // 2. Comprobar que NO se trajo la transacción tx3
        boolean containsTx3 = result.stream().anyMatch(t -> t.getId().equals(tx3.getId()));
        assertEquals(false, containsTx3, "El historial no debe exponer transacciones ajenas");

        // 3. Comprobar el orden (ORDER BY createdAt DESC)
        assertEquals(tx2.getId(), result.get(0).getId()); // tx2 es más reciente (-50s) que tx1 (-100s)
        assertEquals(tx1.getId(), result.get(1).getId());
    }
}
