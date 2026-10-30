package com.cooperativa.met.domain.identity.model;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonNameTest {

    @Test
    void separaNombreYApellido() {
        assertEquals(new PersonName("Juan", "Pérez"), PersonName.fromFullName("Juan Pérez"));
    }

    @Test
    void tomaLasDosUltimasPalabrasComoApellidos() {
        assertEquals(new PersonName("Carlos", "Pérez Gómez"), PersonName.fromFullName("Carlos Pérez Gómez"));
        assertEquals(new PersonName("Juan Carlos", "Pabón Gómez"), PersonName.fromFullName("Juan Carlos Pabón Gómez"));
    }

    @Test
    void mantieneLasParticulasConLaPalabraALaQuePertenecen() {
        assertEquals(new PersonName("María del Carmen", "Pérez Gómez"),
                PersonName.fromFullName("María del Carmen Pérez Gómez"));
        assertEquals(new PersonName("Juan", "De la Cruz Rodríguez"),
                PersonName.fromFullName("Juan De la Cruz Rodríguez"));
        assertEquals(new PersonName("Ana", "de la Hoz"), PersonName.fromFullName("Ana de la Hoz"));
    }

    @Test
    void ignoraLosEspaciosDeMas() {
        assertEquals(new PersonName("Ana María", "López Ruiz"),
                PersonName.fromFullName("  Ana   María  López   Ruiz "));
    }

    @Test
    void rechazaNombresSinApellido() {
        for (String invalid : new String[]{null, "", "   ", "Juan", "de la", "Juan de"}) {
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> PersonName.fromFullName(invalid));
            assertEquals("INVALID_FULL_NAME", ex.getCode());
        }
    }
}
