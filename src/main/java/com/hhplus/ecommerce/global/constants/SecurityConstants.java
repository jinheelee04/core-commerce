package com.hhplus.ecommerce.global.constants;

/**
 * 보안 관련 상수 정의
 * OpenAPI Security Scheme 등
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // 인스턴스화 방지
    }

    /**
     * OpenAPI Security Scheme 이름
     * @SecurityRequirement(name = ...) 에서 사용
     */
    public static final String SECURITY_SCHEME_NAME = "X-User-Id";
}
