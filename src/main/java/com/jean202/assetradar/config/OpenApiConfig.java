package com.jean202.assetradar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Asset Radar API")
                        .description("실시간 자산 가격 수집·분석·알림 플랫폼")
                        .version("0.1.0"));
    }
}
