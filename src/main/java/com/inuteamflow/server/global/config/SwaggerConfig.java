package com.inuteamflow.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final List<Server> SERVER_LIST = List.of(
            createServer("http://localhost:8080", "Local Server"),
            createServer("https://teamflow-dev.inuappcenter.kr", "Development Server")
            // Production 서버 배포 시 주석 해제
            // createServer("https://teamflow.inuappcenter.kr", "Production Server")
            );

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .name("JWT Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .info(apiInfo())
                .servers(SERVER_LIST);
    }

    private Info apiInfo() {
        return new Info()
                .title("INU-TeamFlow API Documentation")
                .description("인천대학교 팀 프로젝트 협업 및 일정 관리 서비스 INU-TeamFlow의 API 문서입니다.")
                .version("1.0.0");
    }

    private static Server createServer(String url, String description) {
        return new Server().url(url).description(description);
    }
}
