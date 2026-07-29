package com.cooperativa.met.infrastructure.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

/**
 * Estrategia de migración exclusiva del perfil {@code dev}.
 *
 * <p>En desarrollo local la base de datos vive en un volumen de Docker que
 * sobrevive a los cambios del repositorio. Cuando un archivo de migración ya
 * aplicado se edita o se renombra, Flyway aborta el arranque con
 * "Migrations have failed validation" (checksum/description mismatch) y el
 * backend nunca levanta.
 *
 * <p>Aquí ejecutamos {@code repair()} antes de {@code migrate()} para realinear
 * el historial con los archivos del repositorio. NO se activa en producción:
 * allí la validación estricta es intencional y un mismatch debe fallar el
 * despliegue.
 */
@Slf4j
@Configuration
@Profile("dev")
public class FlywayDevConfig {

    @Bean
    public FlywayMigrationStrategy repairBeforeMigrate() {
        return flyway -> {
            log.warn("Perfil dev: ejecutando flyway repair() antes de migrate() " +
                     "para realinear el historial con los archivos de migración.");
            flyway.repair();
            flyway.migrate();
        };
    }
}
