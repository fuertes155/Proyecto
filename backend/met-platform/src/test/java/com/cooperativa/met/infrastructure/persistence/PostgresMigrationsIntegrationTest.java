package com.cooperativa.met.infrastructure.persistence;

import com.cooperativa.met.TestRedisConfig;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las migraciones de Flyway contra un PostgreSQL 16 real, la versión de producción en Render. El resto de
 * pruebas usa una base en memoria creada desde las entidades, así que nunca ejecuta estas migraciones.
 *
 * <p>La aplicación arranca como en producción: Flyway crea el esquema y Hibernate solo lo valida
 * ({@code ddl-auto=validate}). Si una migración falla, o una entidad pide una tabla o columna que ninguna
 * migración crea, la prueba falla aquí y no al desplegar.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class PostgresMigrationsIntegrationTest {

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();
    private static final String HASH_DEL_SUPER_ADMIN = new BCryptPasswordEncoder(4).encode("clave-de-prueba-larga");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void usarElPostgresDePrueba(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        propiedades.add("spring.datasource.username", () -> "postgres");
        propiedades.add("spring.datasource.password", () -> "");
        propiedades.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        propiedades.add("SUPERADMIN_PASSWORD_HASH", () -> HASH_DEL_SUPER_ADMIN);
    }

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo arrancar el PostgreSQL de prueba", e);
        }
    }

    @Test
    void aplicaTodasLasMigracionesYCadaEntidadEncuentraSusTablasYColumnas() throws IOException {
        // Si el contexto arrancó, Hibernate ya validó el esquema contra las entidades.
        MigrationInfo[] aplicadas = flyway.info().applied();
        int archivos = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/V*.sql").length;

        assertEquals(archivos, aplicadas.length, "cada archivo de migración debe quedar aplicado");
        assertTrue(Arrays.stream(aplicadas).noneMatch(m -> m.getState().isFailed()));
        assertEquals("16", jdbc.queryForObject("SHOW server_version", String.class).split("\\.")[0]);
    }

    @Test
    void elSuperAdministradorQuedaActivoConElHashConfigurado() {
        Map<String, Object> admin = jdbc.queryForMap(
                "SELECT password_hash, status, role FROM admins WHERE username = 'admin'");

        assertEquals(HASH_DEL_SUPER_ADMIN, admin.get("password_hash"));
        assertEquals("ACTIVE", admin.get("status"));
        assertEquals("SUPER_ADMIN", admin.get("role"));
    }

    @Test
    void sinHashConfiguradoLasMigracionesDejanAlSuperAdministradorConLaClaveAdmin123() {
        // Comportamiento actual (riesgo): V40 pone 'admin123' cuando SUPERADMIN_PASSWORD_HASH llega vacío
        // o mal formado. Una base creada así queda con usuario admin y una clave conocida.
        jdbc.execute("CREATE DATABASE met_sin_hash");
        Flyway.configure()
                .dataSource(POSTGRES.getDatabase("postgres", "met_sin_hash"))
                .locations("classpath:db/migration")
                .placeholders(Map.of("superadmin_password_hash", ""))
                .load()
                .migrate();

        Boolean claveConocida = new JdbcTemplate(POSTGRES.getDatabase("postgres", "met_sin_hash")).queryForObject(
                "SELECT password_hash = crypt('admin123', password_hash) FROM admins WHERE username = 'admin'",
                Boolean.class);

        assertTrue(claveConocida);
    }
}
