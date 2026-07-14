package com.cooperativa.met.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuración de OpenAPI / Swagger UI.
 * Solo disponible en el perfil "dev" — no se expone en producción.
 *
 * <p>Acceso en local: <a href="http://localhost:8080/api/swagger-ui.html">Swagger UI</a></p>
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI metOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MET Platform API")
                        .version("1.0.0")
                        .description("API REST de la Cooperativa Financiera MET.\n\n" +
                                "**Nota:** Esta documentación solo está disponible en entorno de desarrollo.")
                        .contact(new Contact()
                                .name("Equipo MET")
                                .email("dev@met.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en /v1/auth/login")));
    }
}
