package com.assignment.patient.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI patientOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Patient Management API")
                        .description("CRUD API for patient records — Kubernetes assignment microservice")
                        .version("1.0.0"));
    }
}
