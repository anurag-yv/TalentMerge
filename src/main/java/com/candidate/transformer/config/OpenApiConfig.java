package com.candidate.transformer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Multi-Source Candidate Data Transformer API")
                        .version("1.0.0")
                        .description("Enterprise API for parsing, normalising, matching, and transforming candidate records from CSVs and PDF resumes into canonical profiles.")
                        .contact(new Contact()
                                .name("Enterprise Engineering Team")
                                .email("engineering@candidate-transformer.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/api").description("Default API context path")
                ));
    }
}
