package org.openfamilycompass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("OpenFamilyCompass API")
                                                .description(
                                                                "REST API for OpenFamilyCompass.<br/>" +
                                                                                "To authenticate:\n" +
                                                                                "1. Use `POST /api/v1/auth/login` with username/password to get an Access Token.\n"
                                                                                +
                                                                                "2. Click 'Authorize' button at the top.\n"
                                                                                +
                                                                                "3. Enter the token in the format: `Bearer <your-token>`")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("OpenFamilyCompass")
                                                                .url("https://github.com/openfamilycompass/openfamilycompass"))
                                                .license(new License()
                                                                .name("AGPL-3.0")
                                                                .url("https://www.gnu.org/licenses/agpl-3.0.html")))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("Enter JWT token")));
        }
}
