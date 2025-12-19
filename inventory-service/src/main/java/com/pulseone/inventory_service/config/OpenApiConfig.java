package com.pulseone.inventory_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Configuration class for OpenAPI/Swagger documentation for Inventory Service
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PulseOne Inventory Service API")
                        .version("1.0.0")
                        .description(
                                "Inventory and stock management service for PulseOne healthcare platform. Manages pharmaceutical inventory, stock levels, and medication dispensing.")
                        .contact(new Contact()
                                .name("PulseOne Team")
                                .email("support@pulseone.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8084").description("Development server"),
                        new Server().url("http://localhost:8000/inventory").description("API Gateway")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
