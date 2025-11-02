package com.hhplus.ecommerce.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "E-commerce Core Service API",
                version = "1.0.0",
                description = """
                        E-commerce Core Service의 RESTful API 명세서입니다.

                        ## 주요 기능
                        - 상품 조회 및 검색
                        - 장바구니 관리
                        - 주문 생성 및 조회
                        - 결제 처리 (Mock PG)
                        - 쿠폰 발급 및 사용

                        ## 인증
                        현재 버전에서는 `X-User-Id` 헤더를 통해 사용자를 식별합니다.
                        실제 운영 환경에서는 JWT 등의 인증 방식으로 대체되어야 합니다.

                        우측 상단의 **Authorize** 버튼을 클릭하여 X-User-Id를 입력하면,
                        모든 API 요청에 자동으로 포함됩니다.
                        """,
                contact = @Contact(
                        name = "API Support",
                        email = "support@example.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "로컬 개발 서버")
        }
)
@SecurityScheme(
        name = "X-User-Id",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-User-Id",
        description = """
                사용자 ID를 헤더로 전달합니다.

                **예시:** `123`

                실제 운영 환경에서는 JWT Bearer Token 등으로 대체되어야 합니다.
                """
)
public class OpenApiConfig {
}
