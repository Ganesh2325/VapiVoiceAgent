package com.voiceos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI configuration.
 * Accessible at /swagger-ui/index.html in development.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI voiceOsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VoiceOS API")
                        .description("""
                                ## VoiceOS — Multi-Agent Voice AI Operations Platform
                                
                                REST API for VoiceOS. All endpoints (except auth) require a Bearer JWT token.
                                
                                ### Authentication
                                1. POST `/api/v1/auth/register` to create an account
                                2. POST `/api/v1/auth/login` to receive a JWT token
                                3. Add `Authorization: Bearer <token>` header to all subsequent requests
                                
                                ### Mock Mode
                                When `MOCK_EXTERNAL_APIS=true`, all external API calls return clearly labeled `[MOCK]` data.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("VoiceOS")
                                .url("https://github.com/voiceos"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.voiceos.dev").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token obtained from /api/v1/auth/login")
                        )
                );
    }
}
