package com.cooperativa.met.application.admin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Estos campos van a primitivos (boolean/long) en el dominio. Si llegan vacíos, el desempaquetado
 * reventaba con NullPointerException (HTTP 500); deben rechazarse antes como error de validación (400).
 */
class AdminRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void feeMustSayWhetherItIsAPercentage() {
        assertEquals(Set.of("esPorcentaje"), invalidFields(
                new FeeScheduleRequest("RETIRO_PSE", null, new BigDecimal("2500"), null, Instant.now())));
        assertTrue(invalidFields(new FeeScheduleRequest("RETIRO_PSE", null, new BigDecimal("2500"), false, Instant.now())).isEmpty());
    }

    @Test
    void operationLimitNeedsBothAmounts() {
        assertEquals(Set.of("montoDiarioMax", "montoPorTransaccionMax"),
                invalidFields(new OperationLimitRequest("TRANSFERENCIA", null, null)));
        assertTrue(invalidFields(new OperationLimitRequest("TRANSFERENCIA", 5_000_000L, 2_000_000L)).isEmpty());
    }

    private static Set<String> invalidFields(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
