package com.cooperativa.met.domain.identity.model;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void shouldReportNoEmailWhenAccountWasCreatedWithoutOne() {
        assertFalse(User.builder().build().hasEmail());
        assertFalse(User.builder().email("  ").build().hasEmail());
        assertTrue(User.builder().email("socio@example.com").build().hasEmail());
    }

    @Test
    void shouldRequireEmailForOperationsThatSendSecurityCodes() {
        assertEquals("socio@example.com", User.builder().email("socio@example.com").build().requireEmail());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> User.builder().build().requireEmail());
        assertEquals("EMAIL_REQUIRED", ex.getCode());
    }
}
