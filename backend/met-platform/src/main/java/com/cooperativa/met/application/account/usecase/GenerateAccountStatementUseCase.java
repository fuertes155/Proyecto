package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.AccountStatementResult;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GenerateAccountStatementUseCase {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;

    @Transactional(readOnly = true)
    public AccountStatementResult execute(UUID userId, int year, int month) {
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta para el usuario"));

        YearMonth period = YearMonth.of(year, month);
        Instant start = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CoreTransaction> transactions = transactionRepository.findByAccountId(account.getId()).stream()
                .filter(tx -> !tx.getCreatedAt().isBefore(start) && tx.getCreatedAt().isBefore(end))
                .sorted(Comparator.comparing(CoreTransaction::getCreatedAt))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Fecha,Tipo,Descripcion,Monto,Direccion\n");
        for (CoreTransaction tx : transactions) {
            boolean isCredit = account.getId().equals(tx.getDestinationAccountId());
            csv.append(DATE_FORMAT.format(tx.getCreatedAt())).append(',')
                    .append(tx.getType()).append(',')
                    .append(csvEscape(tx.getConcept())).append(',')
                    .append(tx.getAmount()).append(',')
                    .append(isCredit ? "CREDITO" : "DEBITO")
                    .append('\n');
        }

        String fileName = String.format("extracto_%s_%d%02d.csv", account.getAccountNumber(), year, month);
        return new AccountStatementResult(fileName, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
