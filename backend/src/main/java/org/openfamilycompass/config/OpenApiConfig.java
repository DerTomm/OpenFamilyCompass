package org.openfamilycompass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Value("${app.oauth2.issuer-uri:http://localhost:8080}")
    private String issuerUri;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenFamilyCompass API")
                        .description("REST API for OpenFamilyCompass - A family task management application")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OpenFamilyCompass")
                                .url("https://github.com/openfamilycompass/openfamilycompass"))
                        .license(new License()
                                .name("AGPL-3.0")
                                .url("https://www.gnu.org/licenses/agpl-3.0.html")))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2 Authorization Code Flow with PKCE")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(issuerUri + "/oauth2/authorize")
                                                .tokenUrl(issuerUri + "/oauth2/token")
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect")
                                                        .addString("profile", "User profile")
                                                        .addString("read", "Read access")
                                                        .addString("write", "Write access"))))));
    }
}
