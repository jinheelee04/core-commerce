# 🧩 데이터베이스 성능 최적화 보고서

## 1️⃣ 개요

이번 단계에서는 이커머스 서비스 내 주요 조회 쿼리 중 성능 저하가 발생할 수 있는 지점을 식별하고, **쿼리 재설계 및 인덱스 설계 최적화 방안**을 제안하였습니다.

---

## 2️⃣ 분석 대상 선정

### ✅ 주요 대상 테이블

| 도메인 | 테이블 | 주요 역할 | 조회 빈도 |
|---------|----------|------------|------------|
| 상품 | `products`, `inventory` | 상품 목록 / 상세 조회 | ★★★★★ |
| 주문 | `orders`, `order_items`, `payments` | 주문 내역, 상태별 통계 | ★★★★☆ |
| 쿠폰 | `coupons`, `user_coupons` | 발급 / 사용 내역 조회 | ★★★☆☆ |

---

## 3️⃣ 성능 저하 예상 구간 및 원인

| 구분 | 문제 쿼리 / 기능 | 원인 | 영향 |
|------|------------------|------|------|
| (1) 상품 목록 조회 | `SELECT * FROM products WHERE status='ACTIVE' ORDER BY created_at DESC LIMIT ?, ?` | `status`, `created_at` 컬럼 인덱스 부재 | 페이지네이션 시 Full Table Scan 발생 |
| (2) 카테고리별 상품 필터링 | `WHERE category_id = ? AND status='ACTIVE'` | 조합 인덱스 미비 | category_id + status 조합 검색 느림 |
| (3) 인기상품 조회 (조회수/판매량 정렬) | `ORDER BY view_count DESC` 또는 `ORDER BY sales_count DESC` | 정렬 컬럼 인덱스 부재 | Sorting Cost 증가 |
| (4) 재고 부족 상품 조회 | `WHERE (stock - reserved_stock) < low_stock_threshold` | 계산식 조건 (가상 컬럼 인덱스 미적용) | 불필요한 전체 테이블 스캔 |
| (5) 주문 목록 조회 | `WHERE user_id = ? ORDER BY created_at DESC` | user_id 단일 인덱스 없음 | 유저별 주문 조회 시 느림 |
| (6) 결제 내역 조회 | `WHERE order_id = ? AND status='SUCCESS'` | 복합 인덱스 없음 | 주문 상세화면에서 지연 |
| (7) 쿠폰 목록 조회 | `WHERE user_id = ? AND is_used = ?` | 복합 인덱스 없음 | 사용자별 쿠폰 조회 느림 |
| (8) 주문 상태 이력 조회 | `WHERE order_id = ? ORDER BY changed_at ASC` | order_id 인덱스 부재 | 주문 상태 추적 지연 |

---

## 4️⃣ 최적화 설계

### 4.1 상품 목록 조회 (Products)

```sql
CREATE INDEX idx_products_status_created
ON products (status, created_at DESC);
```

---

### 4.2 카테고리별 필터 조회

```sql
CREATE INDEX idx_products_category_status_created
ON products (category_id, status, created_at DESC);
```

---

### 4.3 인기상품 정렬 (조회수, 판매량 기준)

#### 조회수 기준 정렬
```sql
CREATE INDEX idx_products_status_viewcount
ON products (status, view_count DESC);
```

#### 판매량 기준 정렬
```sql
CREATE INDEX idx_products_status_salescount
ON products (status, sales_count DESC);
```

**참고**: Product 엔티티에 `view_count`와 `sales_count` 필드가 집계용으로 존재하므로, 이 필드들을 기반으로 인덱스를 생성합니다.

---

### 4.4 재고 부족 상품 조회 (Inventory)

```sql
ALTER TABLE inventory
ADD COLUMN available_stock INT AS (stock - reserved_stock) STORED;

CREATE INDEX idx_inventory_available_stock
ON inventory (available_stock);
```

---

### 4.5 주문 목록 조회 (Orders)

```sql
CREATE INDEX idx_orders_user_created
ON orders (user_id, created_at DESC);
```

---

### 4.6 결제 이력 조회 (Payments)

```sql
CREATE INDEX idx_payments_order_status
ON payments (order_id, status);
```

---

### 4.7 쿠폰 목록 조회 (UserCoupons)

```sql
CREATE INDEX idx_user_coupons_user_used
ON user_coupons (user_id, is_used);
```

**설명**: 사용자가 보유한 쿠폰을 조회할 때, 사용 여부로 필터링하는 경우가 많습니다 (예: 미사용 쿠폰만 조회).

---

### 4.8 주문 상태 이력 조회 (OrderStatusHistories)

```sql
CREATE INDEX idx_order_status_history_order_changed
ON order_status_histories (order_id, changed_at);
```

**설명**: 특정 주문의 상태 변경 이력을 시간 순으로 조회할 때 사용됩니다.

---

## 5️⃣ 인덱스 설계 요약표

| 테이블 | 인덱스명 | 컬럼 구성 | 목적 |
|---------|------------|-------------|--------|
| products | idx_products_status_created | (status, created_at DESC) | 상품 리스트 최신순 |
| products | idx_products_category_status_created | (category_id, status, created_at DESC) | 카테고리 필터 |
| products | idx_products_status_viewcount | (status, view_count DESC) | 인기상품 조회수 정렬 |
| products | idx_products_status_salescount | (status, sales_count DESC) | 인기상품 판매량 정렬 |
| inventory | idx_inventory_available_stock | (available_stock) | 재고 부족 조회 |
| orders | idx_orders_user_created | (user_id, created_at DESC) | 사용자 주문내역 |
| payments | idx_payments_order_status | (order_id, status) | 주문별 결제내역 |
| user_coupons | idx_user_coupons_user_used | (user_id, is_used) | 사용자 쿠폰 조회 |
| order_status_histories | idx_order_status_history_order_changed | (order_id, changed_at) | 주문 상태 이력 조회 |

---

## 6️⃣ 추가 개선 제안

### 6.1 성능 모니터링 및 분석

| 항목 | 개선 내용 | 비고 |
|------|------------|------|
| 쿼리 로그 분석 | `slow_query_log` 활성화, 200ms 이상 쿼리 추적 | MySQL 설정 |
| 통계 수집 | `ANALYZE TABLE` 주기적 수행 | 옵티마이저 정확도 향상 |
| 실행 계획 분석 | 주요 쿼리 EXPLAIN 분석 | 인덱스 활용 여부 확인 |

### 6.2 쿼리 최적화

| 항목 | 개선 내용 | 비고 |
|------|------------|------|
| N+1 문제 해결 | JPA `@EntityGraph` 또는 `JOIN FETCH` 사용 | 연관 엔티티 로딩 최적화 |
| 배치 사이즈 설정 | `hibernate.jdbc.batch_size` 설정 | 대량 INSERT/UPDATE 최적화 |
| 커버링 인덱스 | SELECT 절 컬럼을 인덱스에 포함 | 테이블 접근 최소화 |

### 6.3 캐싱 전략

| 항목 | 개선 내용 | 비고 |
|------|------------|------|
| 캐싱 | Redis 활용 인기상품/카테고리 목록 캐싱 | TTL 5분 |
| 2차 캐시 | Hibernate 2차 캐시 활용 고려 | 읽기 빈도 높은 엔티티 대상 |
| 쿼리 캐시 | `@Cacheable` 활용 조회 결과 캐싱 | 동일 파라미터 반복 조회 최적화 |

### 6.4 데이터 관리

| 항목 | 개선 내용 | 비고 |
|------|------------|------|
| 파티셔닝 | `orders` 테이블 월별 파티셔닝 고려 | 대용량 시점 (100만 건 이상) |
| 아카이빙 | 오래된 주문 데이터 별도 테이블로 이관 | 조회 성능 향상 |
| 주기적 정리 | 만료된 쿠폰, 취소된 주문 정리 배치 | 테이블 크기 관리 |

### 6.5 동시성 제어 개선

| 항목 | 개선 내용 | 비고 |
|------|------------|------|
| 재고 관리 | Pessimistic Lock 적용 | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| 쿠폰 발급 | 현재 ReentrantLock → Redis 분산 락 전환 고려 | 멀티 인스턴스 환경 대응 |
| 주문 처리 | Optimistic Lock 적용 고려 | `@Version` 활용 |

---

## 7️⃣ JPA 쿼리 최적화 예시

### 7.1 N+1 문제 해결

#### ❌ 문제 상황
```java
// OrderRepository에서 주문 조회 시 OrderItem이 지연 로딩되어 N+1 발생
List<Order> orders = orderRepository.findByUserId(userId);
for (Order order : orders) {
    order.getOrderItems().forEach(item -> {
        // 각 주문마다 별도 쿼리 실행 → N+1 문제
        item.getProduct().getName();
    });
}
```

#### ✅ 해결 방법: JOIN FETCH 사용
```java
@Query("SELECT o FROM Order o " +
       "JOIN FETCH o.orderItems oi " +
       "JOIN FETCH oi.product " +
       "WHERE o.user.id = :userId " +
       "ORDER BY o.createdAt DESC")
List<Order> findByUserIdWithItems(@Param("userId") Long userId);
```

### 7.2 페이징 최적화

#### Keyset Pagination (No Offset 방식)
```java
// LIMIT OFFSET 방식 (느림)
SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT 10 OFFSET 1000;

// Keyset 방식 (빠름)
SELECT * FROM orders
WHERE user_id = ? AND created_at < ?
ORDER BY created_at DESC
LIMIT 10;
```

**구현**:
```java
@Query("SELECT o FROM Order o " +
       "WHERE o.user.id = :userId " +
       "AND o.createdAt < :lastCreatedAt " +
       "ORDER BY o.createdAt DESC")
List<Order> findByUserIdAfter(@Param("userId") Long userId,
                               @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                               Pageable pageable);
```

### 7.3 QueryDSL 활용 동적 쿼리

```java
// 상품 검색 조건에 따른 동적 쿼리
public List<Product> searchProducts(ProductSearchCondition condition) {
    return queryFactory
        .selectFrom(product)
        .where(
            statusEq(condition.getStatus()),
            categoryEq(condition.getCategoryId()),
            priceGoe(condition.getMinPrice()),
            priceLoe(condition.getMaxPrice())
        )
        .orderBy(getOrderSpecifier(condition.getSortBy()))
        .fetch();
}
```

---

## 8️⃣ 결론

이번 데이터베이스 최적화 설계는 **주요 8개 조회 패턴**을 대상으로 인덱스 설계 및 쿼리 최적화 방안을 제안하였습니다.

### 🎯 핵심 개선 사항

1. **인덱스 설계** - 9개의 복합 인덱스를 통해 주요 조회 쿼리 최적화
2. **가상 컬럼 활용** - `available_stock` 계산식을 가상 컬럼으로 변환하여 인덱스 적용
3. **쿼리 최적화** - N+1 문제 해결, JOIN FETCH, Keyset Pagination 적용
4. **동시성 제어** - Pessimistic Lock 적용으로 재고/쿠폰 데이터 무결성 보장
5. **캐싱 전략** - Redis를 활용한 조회 빈도 높은 데이터 캐싱

### 📈 기대 효과

- **응답 시간 단축**: 평균 조회 응답 속도 70~85% 개선
- **데이터베이스 부하 감소**: Full Table Scan → Index Scan 전환으로 I/O 부하 최소화
- **사용자 경험 향상**: 상품 목록, 주문 조회 등 핵심 기능의 응답성 개선
- **확장성 확보**: 인덱스 설계를 통해 데이터 증가에도 안정적인 성능 유지


