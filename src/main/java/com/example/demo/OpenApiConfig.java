package com.example.demo;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Title and description shown at the top of the Swagger UI page.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI employeeManagementApi() {
        return new OpenAPI().info(new Info()
                .title("Employee Management API")
                .version("v1")
                .description("REST API for employees and departments. "
                        + "Errors return a JSON body with a message and, "
                        + "for field problems, an errors map."));
    }
}
