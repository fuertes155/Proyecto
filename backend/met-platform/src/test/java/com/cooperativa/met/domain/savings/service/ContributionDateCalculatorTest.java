package com.cooperativa.met.domain.savings.service;

import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContributionDateCalculatorTest {

    @Test
    void shouldCalculateNextWeeklyDateOnSameDay() {
        LocalDate monday = LocalDate.of(2026, 6, 8);
        LocalDate result = ContributionDateCalculator.calculateInitialNextDate(
                ContributionFrequency.WEEKLY, DayOfWeek.MONDAY.getValue(), null, monday);
        assertEquals(monday, result);
    }

    @Test
    void shouldCalculateNextMonthlyDateInSameMonth() {
        LocalDate from = LocalDate.of(2026, 6, 5);
        LocalDate result = ContributionDateCalculator.calculateInitialNextDate(
                ContributionFrequency.MONTHLY, null, 15, from);
        assertEquals(LocalDate.of(2026, 6, 15), result);
    }

    @Test
    void shouldCalculateBiweeklyNextDate() {
        LocalDate after = LocalDate.of(2026, 6, 8);
        LocalDate result = ContributionDateCalculator.calculateNextDate(
                ContributionFrequency.BIWEEKLY, DayOfWeek.MONDAY.getValue(), null, after);
        assertEquals(LocalDate.of(2026, 6, 22), result);
    }
}
