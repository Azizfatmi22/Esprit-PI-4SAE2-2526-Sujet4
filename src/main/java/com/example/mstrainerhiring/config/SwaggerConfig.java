package com.example.mstrainerhiring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI partnerHiringOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Partner Hiring Microservice API")
                        .description("REST API for managing partner hiring, including document uploads and partner management")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MS-TrainerHiring Team")
                                .email("contact@mstrainerhiring.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ));
    }
}
