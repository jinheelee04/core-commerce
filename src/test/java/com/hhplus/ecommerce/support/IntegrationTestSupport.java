package com.hhplus.ecommerce.support;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 기본 클래스
 * - Testcontainers MySQL 사용
 * - 각 테스트마다 트랜잭션 롤백 (@Transactional)
 * - 실제 DB 환경과 동일한 테스트 가능
 */
@SpringBootTest
@Testcontainers
@Transactional
public abstract class IntegrationTestSupport {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    protected EntityManager em;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 영속성 컨텍스트 초기화
        em.clear();
    }

    /**
     * 영속성 컨텍스트를 flush하고 clear하여 DB 동기화
     */
    protected void flushAndClear() {
        em.flush();
        em.clear();
    }
}
