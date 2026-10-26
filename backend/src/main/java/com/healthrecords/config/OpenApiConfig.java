package com.healthrecords.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation. Served at /v3/api-docs and /swagger-ui.html.
 * In production, restrict the Swagger UI endpoints in SecurityConfig /
 * at the reverse proxy — they are currently permitted for portfolio use.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Health Records Management System API",
                version = "1.0.0",
                description = "JWT-authenticated API for medical records, lab reports and prescriptions. "
                        + "Patients see only their own data; doctors and admins see all patients."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
