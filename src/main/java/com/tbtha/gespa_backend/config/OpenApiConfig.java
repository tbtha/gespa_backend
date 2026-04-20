package com.tbtha.gespa_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gespaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GESPA API")
                        .description("API REST para Gestión de Pacientes para Profesionales de Salud")
                        .version("v1")
                        .license(new License().name("Uso académico")));
    }
}
