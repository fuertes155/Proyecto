package com.cooperativa.fintech.domain.savings.service;

import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.savings.model.ContributionFrequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class ContributionDateCalculator {

    private ContributionDateCalculator() {
    }

    public static LocalDate calculateInitialNextDate(
            ContributionFrequency frequency,
            Integer debitDayOfWeek,
            Integer debitDayOfMonth,
            LocalDate fromDate) {
        return switch (frequency) {
            case WEEKLY, BIWEEKLY -> calculateNextWeeklyDate(debitDayOfWeek, fromDate);
            case MONTHLY -> calculateNextMonthlyDate(debitDayOfMonth, fromDate);
        };
    }

    public static LocalDate calculateNextDate(
            ContributionFrequency frequency,
            Integer debitDayOfWeek,
            Integer debitDayOfMonth,
            LocalDate afterDate) {
        LocalDate base = afterDate.plusDays(1);
        return switch (frequency) {
            case WEEKLY -> calculateNextWeeklyDate(debitDayOfWeek, base);
            case BIWEEKLY -> calculateNextWeeklyDate(debitDayOfWeek, base).plusWeeks(1);
            case MONTHLY -> calculateNextMonthlyDate(debitDayOfMonth, base);
        };
    }

    private static LocalDate calculateNextWeeklyDate(Integer debitDayOfWeek, LocalDate fromDate) {
        if (debitDayOfWeek == null || debitDayOfWeek < 1 || debitDayOfWeek > 7) {
            throw new BusinessRuleException("INVALID_DEBIT_DAY", "Día de la semana inválido (1-7)");
        }
        DayOfWeek target = DayOfWeek.of(debitDayOfWeek);
        if (fromDate.getDayOfWeek() == target) {
            return fromDate;
        }
        return fromDate.with(TemporalAdjusters.nextOrSame(target));
    }

    private static LocalDate calculateNextMonthlyDate(Integer debitDayOfMonth, LocalDate fromDate) {
        if (debitDayOfMonth == null || debitDayOfMonth < 1 || debitDayOfMonth > 28) {
            throw new BusinessRuleException("INVALID_DEBIT_DAY", "Día del mes inválido (1-28)");
        }
        LocalDate candidate = fromDate.withDayOfMonth(Math.min(debitDayOfMonth, fromDate.lengthOfMonth()));
        if (!candidate.isBefore(fromDate)) {
            return candidate;
        }
        LocalDate nextMonth = fromDate.plusMonths(1);
        return nextMonth.withDayOfMonth(Math.min(debitDayOfMonth, nextMonth.lengthOfMonth()));
    }
}
