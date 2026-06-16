package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.application.savings.dto.CreateScheduledSavingsRequest;
import com.cooperativa.met.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.met.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.port.SavingsBalanceCachePort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import com.cooperativa.met.domain.savings.service.ContributionDateCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateScheduledSavingsUseCase {

    private final UserRepositoryPort userRepository;
    private final ScheduledSavingsAccountPort accountPort;
    private final SavingsBalanceCachePort balanceCachePort;
    private final ScheduledSavingsMapper mapper;

    @Transactional
    public ScheduledSavingsResponse execute(UUID userId, CreateScheduledSavingsRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Usuario no encontrado"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "La cuenta debe estar activa para crear ahorro programado");
        }

        validateDebitSchedule(request);

        LocalDate nextDate = ContributionDateCalculator.calculateInitialNextDate(
                request.frequency(),
                request.debitDayOfWeek(),
                request.debitDayOfMonth(),
                LocalDate.now()
        );

        ScheduledSavingsAccount account = ScheduledSavingsAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .contributionAmount(request.contributionAmount())
                .frequency(request.frequency())
                .debitDayOfWeek(request.debitDayOfWeek())
                .debitDayOfMonth(request.debitDayOfMonth())
                .currentBalance(BigDecimal.ZERO)
                .status(ScheduledSavingsStatus.ACTIVE)
                .nextContributionDate(nextDate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ScheduledSavingsAccount saved = accountPort.save(account);
        balanceCachePort.cacheBalance(saved.getId(), saved.getCurrentBalance());
        return mapper.toResponse(saved);
    }

    private void validateDebitSchedule(CreateScheduledSavingsRequest request) {
        if (request.frequency() == ContributionFrequency.MONTHLY) {
            if (request.debitDayOfMonth() == null) {
                throw new BusinessRuleException("INVALID_SCHEDULE", "Debe indicar el día del mes (1-28)");
            }
            return;
        }
        if (request.debitDayOfWeek() == null) {
            throw new BusinessRuleException("INVALID_SCHEDULE", "Debe indicar el día de la semana (1-7)");
        }
    }
}
