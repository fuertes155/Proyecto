package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.admin.port.AdminRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, avisa si el super administrador inicial todavía entra con la clave {@code admin123}.
 *
 * <p>La migración V40 pone esa clave cuando {@code SUPERADMIN_PASSWORD_HASH} llegó vacío o mal
 * formado; con una clave conocida cualquiera entra al panel. Nunca impide el arranque: deja un
 * error en el log para que se cambie la clave cuanto antes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAdminPasswordCheck implements ApplicationRunner {

    static final String DEFAULT_USERNAME = "admin";
    static final String DEFAULT_PASSWORD = "admin123";

    private final AdminRepositoryPort adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            adminRepository.findByUsername(DEFAULT_USERNAME)
                    .filter(admin -> admin.getPasswordHash() != null
                            && passwordEncoder.matches(DEFAULT_PASSWORD, admin.getPasswordHash()))
                    .ifPresent(admin -> log.error("SEGURIDAD: el administrador '{}' todavía entra con la clave "
                            + "por defecto '{}'. Cámbiala ya guardando un hash BCrypt nuevo en admins.password_hash "
                            + "(el panel todavía no permite cambiarla).", DEFAULT_USERNAME, DEFAULT_PASSWORD));
        } catch (RuntimeException e) {
            log.warn("No se pudo revisar la clave del administrador inicial: {}", e.getMessage());
        }
    }
}
