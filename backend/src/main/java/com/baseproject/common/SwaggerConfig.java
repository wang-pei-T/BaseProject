package com.baseproject.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI baseProjectOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("BaseProject API")
                        .version("v1.0")
                        .description("BaseProject backend API document")
                        .contact(new Contact().name("BaseProject"))
        );
    }
}

