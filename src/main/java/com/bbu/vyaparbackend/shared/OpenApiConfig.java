package com.bbu.vyaparbackend.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI vyaparOpenApi() {
        String bearerScheme = "bearerAuth";
        String refreshScheme = "refreshCookie";
        Components components = new Components()
                .addSecuritySchemes(bearerScheme, new SecurityScheme()
                        .name(bearerScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(refreshScheme, new SecurityScheme()
                        .name("vyapar_refresh")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE));
        return new OpenAPI()
                .info(new Info().title("Vyapar Operations API").version("v1")
                        .description("Multi-business POS, inventory, billing, and reporting API"))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme));
    }
}
