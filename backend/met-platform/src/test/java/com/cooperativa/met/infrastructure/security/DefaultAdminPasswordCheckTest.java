package com.cooperativa.met.infrastructure.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cooperativa.met.domain.admin.model.Admin;
import com.cooperativa.met.domain.admin.port.AdminRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Aviso al arrancar si el super administrador sigue con la clave admin123 (ver V40). */
class DefaultAdminPasswordCheckTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AdminRepositoryPort admins = mock(AdminRepositoryPort.class);
    private final DefaultAdminPasswordCheck check = new DefaultAdminPasswordCheck(admins, encoder);
    private final Logger logger = (Logger) LoggerFactory.getLogger(DefaultAdminPasswordCheck.class);
    private final ListAppender<ILoggingEvent> registros = new ListAppender<>();

    @BeforeEach
    void escucharElLog() {
        registros.start();
        logger.addAppender(registros);
    }

    @AfterEach
    void dejarDeEscuchar() {
        logger.detachAppender(registros);
    }

    private void adminConClave(String clave) {
        when(admins.findByUsername("admin"))
                .thenReturn(Optional.of(Admin.builder().username("admin").passwordHash(encoder.encode(clave)).build()));
    }

    private List<ILoggingEvent> errores() {
        return registros.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
    }

    @Test
    void avisaConUnErrorSiElAdministradorSigueConLaClaveAdmin123() {
        adminConClave("admin123");

        check.run(null);

        assertEquals(1, errores().size());
        assertTrue(errores().get(0).getFormattedMessage().contains("admin123"));
    }

    @Test
    void noAvisaSiLaClaveYaSeCambio() {
        adminConClave("una-clave-larga-y-propia");

        check.run(null);

        assertTrue(errores().isEmpty());
    }

    @Test
    void noAvisaSiNoExisteElAdministradorInicial() {
        when(admins.findByUsername("admin")).thenReturn(Optional.empty());

        check.run(null);

        assertTrue(errores().isEmpty());
    }

    @Test
    void nuncaImpideArrancarAunqueNoPuedaConsultarLaBase() {
        when(admins.findByUsername("admin")).thenThrow(new IllegalStateException("La base no responde"));

        assertDoesNotThrow(() -> check.run(null));
        assertTrue(errores().isEmpty());
    }
}
