package com.hhplus.ecommerce.global.constants;

/**
 * HTTP 헤더 상수 정의
 */
public final class HttpHeaders {

    private HttpHeaders() {
        // 인스턴스화 방지
    }

    /**
     * 사용자 식별을 위한 헤더
     * 실제 운영 환경에서는 JWT 토큰 등으로 대체 필요
     */
    public static final String X_USER_ID = "X-User-Id";
}
