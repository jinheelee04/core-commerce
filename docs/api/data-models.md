# 🗄️ Data Models - E-commerce Service

---

## 목차

1. [Entity Relationship Diagram](#entity-relationship-diagram)
2. [엔티티 상세 정의](#엔티티-상세-정의)

---

## Entity Relationship Diagram

### 전체 ERD

```mermaid
erDiagram
    USER ||--o| CART : has
    USER ||--o{ ORDER : places
    USER ||--o{ USER_COUPON : owns

    CART ||--o{ CART_ITEM : contains
    PRODUCT ||--o{ CART_ITEM : in

    ORDER ||--o{ ORDER_ITEM : contains
    ORDER ||--o| PAYMENT : has
    ORDER ||--o| USER_COUPON : uses

    PRODUCT ||--o{ ORDER_ITEM : in
    PRODUCT ||--|| INVENTORY : "has stock"

    COUPON ||--o{ USER_COUPON : "issued as"

    USER {
        bigint id PK
        string email UK
        string name
        string phone
        timestamp created_at
        timestamp updated_at
    }

    PRODUCT {
        bigint id PK
        string name
        text description
        bigint price
        string category
        string brand
        string image_url
        string status
        timestamp created_at
        timestamp updated_at
    }

    CART {
        bigint id PK
        bigint user_id FK
        timestamp created_at
        timestamp updated_at
    }

    CART_ITEM {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
        timestamp created_at
        timestamp updated_at
    }

    ORDER {
        bigint id PK
        bigint user_id FK
        string order_number UK
        string status
        bigint items_total
        bigint discount_amount
        bigint final_amount
        string delivery_address
        text delivery_memo
        timestamp expires_at
        timestamp paid_at
        timestamp cancelled_at
        string cancel_reason
        timestamp created_at
        timestamp updated_at
    }

    ORDER_ITEM {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        bigint unit_price
        bigint subtotal
        timestamp created_at
        timestamp updated_at
    }

    PAYMENT {
        bigint id PK
        bigint order_id FK
        bigint amount
        string payment_method
        string status
        string transaction_id
        text fail_reason
        timestamp paid_at
        timestamp failed_at
        timestamp created_at
        timestamp updated_at
    }

    COUPON {
        bigint id PK
        string code UK
        string name
        text description
        string discount_type
        int discount_value
        bigint min_order_amount
        bigint max_discount_amount
        int total_quantity
        int remaining_quantity
        timestamp starts_at
        timestamp ends_at
        string status
        timestamp created_at
        timestamp updated_at
    }

    USER_COUPON {
        bigint id PK
        bigint coupon_id FK
        bigint user_id FK
        bigint order_id FK
        boolean is_used
        timestamp issued_at
        timestamp used_at
        timestamp expires_at
        timestamp updated_at
    }

    INVENTORY {
        bigint id PK
        bigint product_id FK
        int stock
        int reserved_stock
        int low_stock_threshold
        timestamp created_at
        timestamp updated_at
    }

```

---

## 엔티티 상세 정의

### 1. User (사용자)

**테이블명**: `users`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 사용자 ID (PK) |
| `email` | VARCHAR(255) | NO | - | 이메일 (UK) |
| `name` | VARCHAR(100) | NO | - | 사용자 이름 |
| `phone` | VARCHAR(20) | YES | - | 전화번호 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`
- UNIQUE KEY: `email`

---

### 2. Product (상품)

**테이블명**: `products`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 상품 ID (PK) |
| `name` | VARCHAR(255) | NO | - | 상품명 |
| `description` | TEXT | YES | - | 상품 설명 |
| `price` | BIGINT | NO | - | 가격 (원 단위) |
| `category` | VARCHAR(50) | NO | - | 카테고리 |
| `brand` | VARCHAR(100) | YES | - | 브랜드 |
| `image_url` | VARCHAR(500) | YES | - | 이미지 URL |
| `status` | VARCHAR(20) | NO | 'AVAILABLE' | 상품 상태 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`

**상태 값**
- `AVAILABLE`: 판매 가능
- `OUT_OF_STOCK`: 품절

---

### 3. Cart (장바구니)

**테이블명**: `carts`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 장바구니 ID (PK) |
| `user_id` | BIGINT | NO | - | 사용자 ID (FK) |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`
- UNIQUE KEY: `user_id` (1 사용자 = 1 장바구니)

---USER_COUPON

### 4. CartItem (장바구니 항목)

**테이블명**: `cart_items`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 장바구니 항목 ID (PK) |
| `cart_id` | BIGINT | NO | - | 장바구니 ID (FK) |
| `product_id` | BIGINT | NO | - | 상품 ID (FK) |
| `quantity` | INT | NO | 1 | 수량 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`
- UNIQUE KEY: `(cart_id, product_id)` (장바구니 내 상품 중복 방지)

**주요 컬럼 설명**

- **`UNIQUE KEY (cart_id, product_id)` (장바구니 내 상품 중복 방지)**
  - **목적**: 한 장바구니 내에서 동일 상품이 여러 행으로 중복 저장되는 것 방지
  - **실무 시나리오**:
    - 사용자가 같은 상품을 여러 번 추가하면 수량만 증가
    - 중복된 행이 아닌 단일 행의 `quantity` 업데이트
  - **구현 방식**:
    ```sql
    -- 상품 추가 시 UPSERT 패턴 사용
    INSERT INTO cart_items (cart_id, product_id, quantity)
    VALUES (1, 100, 2)
    ON DUPLICATE KEY UPDATE quantity = quantity + 2;
    ```
  - **데이터 정합성**: 동일 상품이 여러 행에 분산되어 장바구니 총액 계산 오류 방지

---

### 5. Order (주문)

**테이블명**: `orders`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 주문 ID (PK) |
| `user_id` | BIGINT | NO | - | 사용자 ID (FK) |
| `order_number` | VARCHAR(50) | NO | - | 주문번호 (UK) |
| `status` | VARCHAR(20) | NO | 'PENDING' | 주문 상태 |
| `items_total` | BIGINT | NO | 0 | 상품 합계 금액 |
| `discount_amount` | BIGINT | NO | 0 | 할인 금액 |
| `final_amount` | BIGINT | NO | 0 | 최종 결제 금액 |
| `delivery_address` | VARCHAR(500) | NO | - | 배송 주소 |
| `delivery_memo` | TEXT | YES | - | 배송 메모 |
| `expires_at` | TIMESTAMP | YES | - | 만료 시간 (15분) |
| `paid_at` | TIMESTAMP | YES | - | 결제 완료 시간 |
| `cancelled_at` | TIMESTAMP | YES | - | 취소 시간 |
| `cancel_reason` | VARCHAR(255) | YES | - | 취소 사유 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`
- UNIQUE KEY: `order_number`

**상태 값**
- `PENDING`: 결제 대기
- `PAID`: 결제 완료
- `CONFIRMED`: 주문 확정
- `CANCELLED`: 주문 취소

**주문번호 생성 규칙**
```
형식: ORD-YYYYMMDD-{순번}
예시: ORD-20250128-001
```

**주요 컬럼 설명**

- **`order_number` (주문번호)**
  - **목적**: 비즈니스 식별자로서 고객 대응, 인보이스, 배송 추적 등에 사용
  - **왜 PK(id)가 아닌가?**:
    - `id`는 내부 시스템용 기술적 식별자
    - `order_number`는 고객에게 노출되는 비즈니스 식별자
    - 고객 문의 시 "주문번호 ORD-20250128-001"로 소통 가능
    - 시스템 간 연동 시 안전한 외부 노출 가능 (PK 노출 지양)
  - **생성 시점**: 주문 생성 트랜잭션 내에서 원자적으로 생성
  - **중복 방지**: UNIQUE KEY 제약조건으로 중복 생성 차단

- **`expires_at` (만료 시간)**
  - **목적**: 미결제 주문의 자동 취소를 위한 만료 시점 관리
  - **실무 시나리오**:
    - 주문 생성 후 15분 내 미결제 시 재고 해제 필요
    - 배치 작업에서 `expires_at < NOW() AND status = 'PENDING'` 조건으로 취소 처리
  - **재고 관리**: 만료된 주문의 예약 재고를 다시 판매 가능 재고로 전환

- **`items_total`, `discount_amount`, `final_amount`**
  - **목적**: 금액 계산의 추적성 및 감사(Audit) 목적
  - **왜 계산값을 저장하는가?**:
    - 주문 생성 시점의 가격 정보 보존 (상품 가격 변경과 무관)
    - 정산 및 회계 감사 시 원본 데이터로 활용
    - 조회 성능 향상 (매번 계산 불필요)
  - **일관성 보장**: 애플리케이션 레벨에서 계산 후 저장, DB 레벨 검증

---

### 6. OrderItem (주문 항목)

**테이블명**: `order_items`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 주문 항목 ID (PK) |
| `order_id` | BIGINT | NO | - | 주문 ID (FK) |
| `product_id` | BIGINT | NO | - | 상품 ID (FK) |
| `quantity` | INT | NO | - | 수량 |
| `unit_price` | BIGINT | NO | - | 단가 (주문 시점 가격) |
| `subtotal` | BIGINT | NO | - | 소계 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`

**주요 컬럼 설명**

- **`unit_price` (단가)**
  - **목적**: 주문 시점의 상품 가격 스냅샷 저장
  - **왜 Product 테이블의 price를 참조하지 않는가?**:
    - 상품 가격은 시간에 따라 변동 가능 (할인, 프로모션 등)
    - 주문 이후 가격이 변경되어도 주문 내역은 변경되지 않아야 함
    - 법적/회계적으로 주문 당시 가격으로 거래가 성립
  - **실무 사례**:
    - 고객이 10,000원에 주문 후, 상품 가격이 15,000원으로 변경되어도 주문서에는 10,000원 유지
    - 환불/교환 시에도 구매 당시 가격 기준으로 처리

- **`subtotal` (소계)**
  - **목적**: 행 단위 금액 계산 결과 저장
  - **계산식**: `unit_price * quantity`
  - **저장 이유**: 조회 성능 최적화 및 데이터 정합성 검증 기준

---

### 7. Payment (결제)

**테이블명**: `payments`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 결제 ID (PK) |
| `order_id` | BIGINT | NO | - | 주문 ID (FK) |
| `amount` | BIGINT | NO | - | 결제 금액 |
| `payment_method` | VARCHAR(20) | NO | - | 결제 수단 |
| `status` | VARCHAR(20) | NO | 'PENDING' | 결제 상태 |
| `transaction_id` | VARCHAR(100) | YES | - | PG 거래 ID |
| `fail_reason` | TEXT | YES | - | 실패 사유 |
| `paid_at` | TIMESTAMP | YES | - | 결제 완료 시간 |
| `failed_at` | TIMESTAMP | YES | - | 결제 실패 시간 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`

**상태 값**
- `PENDING`: 결제 대기
- `SUCCESS`: 결제 성공
- `FAILED`: 결제 실패

**결제 수단**
- `CARD`: 신용/체크카드
- `VIRTUAL_ACCOUNT`: 가상계좌
- `PHONE`: 휴대폰 결제

**주요 컬럼 설명**

- **`transaction_id` (PG 거래 ID)**
  - **목적**: PG사(Payment Gateway)에서 발급하는 고유 거래 식별자
  - **멱등성 보장**:
    - 네트워크 타임아웃 등으로 동일 결제 요청이 중복 발생할 수 있음
    - PG사의 `transaction_id`를 기준으로 중복 결제 여부 판단
    - 동일 `transaction_id`로 재요청 시 기존 결제 결과 반환 (중복 결제 차단)
  - **실무 시나리오**:
    ```
    1. 클라이언트가 결제 요청 → PG사 응답 대기 중 네트워크 끊김
    2. 클라이언트가 결제 실패로 판단하여 재시도
    3. PG사는 transaction_id로 중복 확인 → 첫 번째 결제 결과 반환
    4. 서버는 transaction_id 기준으로 중복 처리 차단
    ```
  - **외부 시스템 연동**:
    - PG사 관리자 페이지에서 거래 조회 시 사용
    - 정산, 환불, 취소 요청 시 필수 파라미터
  - **NULL 허용 이유**: 결제 생성(PENDING) 시점에는 미발급, PG 요청 후 할당

- **`order_id` UNIQUE 제약조건이 없는 이유**
  - 한 주문에 대해 여러 결제 시도 가능 (실패 후 재결제)
  - 결제 이력 추적을 위해 모든 시도 기록
  - 실제 완료된 결제는 `status = 'SUCCESS'` 조건으로 필터링

- **`amount` (결제 금액)**
  - **목적**: 결제 요청 시점의 금액 저장
  - **검증**: 주문의 `final_amount`와 일치 여부 검증 필수
  - **실패 케이스**: 금액 불일치 시 결제 거부 (위변조 방지)

---

### 8. Coupon (쿠폰)

**테이블명**: `coupons`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 쿠폰 ID (PK) |
| `code` | VARCHAR(50) | NO | - | 쿠폰 코드 (UK) |
| `name` | VARCHAR(255) | NO | - | 쿠폰명 |
| `description` | TEXT | YES | - | 설명 |
| `discount_type` | VARCHAR(20) | NO | - | 할인 타입 |
| `discount_value` | INT | NO | - | 할인 값 |
| `min_order_amount` | BIGINT | NO | 0 | 최소 주문 금액 |
| `max_discount_amount` | BIGINT | YES | - | 최대 할인 금액 |
| `total_quantity` | INT | NO | - | 총 발급 수량 |
| `remaining_quantity` | INT | NO | - | 잔여 수량 |
| `starts_at` | TIMESTAMP | NO | - | 시작일시 |
| `ends_at` | TIMESTAMP | NO | - | 종료일시 |
| `status` | VARCHAR(20) | NO | 'ACTIVE' | 쿠폰 상태 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`
- UNIQUE KEY: `code`

**할인 타입**
- `PERCENTAGE`: 정률 할인 (%, discount_value는 1~100)
- `FIXED_AMOUNT`: 정액 할인 (원)

**상태 값**
- `ACTIVE`: 활성
- `INACTIVE`: 비활성
- `EXPIRED`: 만료됨

**주요 컬럼 설명**

- **`total_quantity` / `remaining_quantity` (총 발급 수량 / 잔여 수량)**
  - **목적**: 선착순 쿠폰의 발급 한도 관리
  - **동시성 제어 필요**:
    - 여러 사용자가 동시에 쿠폰 발급 요청 시 초과 발급 방지
    - 비관적 락(Pessimistic Lock) 또는 낙관적 락(Optimistic Lock) 적용 필요
  - **실무 시나리오**:
    ```
    UPDATE coupons
    SET remaining_quantity = remaining_quantity - 1
    WHERE id = ? AND remaining_quantity > 0
    ```
    - 영향받은 행이 0이면 발급 실패 (재고 소진)

- **`max_discount_amount` (최대 할인 금액)**
  - **목적**: 정률 할인(PERCENTAGE) 시 할인 금액 상한선 설정
  - **실무 사례**:
    - "10% 할인, 최대 5,000원" → `discount_value=10`, `max_discount_amount=5000`
    - 100,000원 상품: 10% = 10,000원 → 상한선 적용 → 5,000원 할인
    - 30,000원 상품: 10% = 3,000원 → 상한선 미적용 → 3,000원 할인

---

### 9. UserCoupon (사용자 쿠폰)

**테이블명**: `user_coupons`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 사용자 쿠폰 ID (PK) |
| `coupon_id` | BIGINT | NO | - | 쿠폰 ID (FK) |
| `user_id` | BIGINT | NO | - | 사용자 ID (FK) |
| `order_id` | BIGINT | YES | - | 주문 ID (FK) |
| `is_used` | BOOLEAN | NO | FALSE | 사용 여부 |
| `issued_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 발급일시 |
| `used_at` | TIMESTAMP | YES | - | 사용일시 |
| `expires_at` | TIMESTAMP | NO | - | 만료일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`
- UNIQUE KEY: `(coupon_id, user_id)` (1인 1매 제한)

**주요 컬럼 설명**

- **`UNIQUE KEY (coupon_id, user_id)` (중복 발급 방지)**
  - **목적**: 동일 사용자가 같은 쿠폰을 중복 발급받는 것을 방지
  - **멱등성 보장**:
    - 사용자가 발급 버튼을 여러 번 클릭해도 1매만 발급
    - DB 레벨 제약조건으로 애플리케이션 로직 오류에도 방어
  - **실무 시나리오**:
    ```sql
    -- 발급 시도
    INSERT INTO user_coupons (coupon_id, user_id, expires_at)
    VALUES (1, 100, '2025-12-31 23:59:59');

    -- 중복 발급 시 UNIQUE 제약조건 위반 에러 발생
    -- 애플리케이션에서 "이미 발급받은 쿠폰입니다" 메시지 반환
    ```
  - **예외 케이스**: 사용 후 재발급 허용하려면 제약조건 수정 필요
    - `(coupon_id, user_id, is_used)` → 미사용 쿠폰만 중복 방지

- **`order_id` (주문 ID)**
  - **목적**: 쿠폰이 어느 주문에 사용되었는지 추적
  - **NULL 허용**: 미사용 쿠폰은 NULL
  - **사용 이력 관리**:
    - 주문 취소 시 쿠폰 복구 가능 여부 판단
    - 쿠폰 사용 통계 및 효과 분석

- **`is_used` / `used_at`**
  - **목적**: 쿠폰 사용 여부 및 사용 시점 기록
  - **동시성 제어**:
    - 사용자가 동시에 여러 주문에 같은 쿠폰 적용 시도 방지
    - 낙관적 락: `UPDATE user_coupons SET is_used = true WHERE id = ? AND is_used = false`

---

### 10. Inventory (재고)

**테이블명**: `inventory`

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|-------|------|------|-------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 재고 ID (PK) |
| `product_id` | BIGINT | NO | - | 상품 ID (FK, UK) |
| `stock` | INT | NO | 0 | 현재 재고 |
| `reserved_stock` | INT | NO | 0 | 예약 재고 |
| `low_stock_threshold` | INT | NO | 10 | 낮은 재고 기준 |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**제약조건**
- PRIMARY KEY: `id`
- UNIQUE KEY: `product_id` (1 상품 = 1 재고)

**계산 필드**
```sql
available_stock = stock - reserved_stock
```

**주요 컬럼 설명**

- **`stock` / `reserved_stock` (현재 재고 / 예약 재고)**
  - **목적**: 재고의 물리적 수량과 예약된 수량을 분리 관리
  - **왜 분리하는가?**:
    - `stock`: 실제 창고에 있는 물리적 재고 수량
    - `reserved_stock`: 주문(PENDING) 상태로 예약된 재고
    - `available_stock = stock - reserved_stock`: 실제 판매 가능한 재고
  - **실무 시나리오**:
    ```
    초기: stock=100, reserved_stock=0, available=100

    [주문 생성]
    → reserved_stock += 5
    → stock=100, reserved_stock=5, available=95

    [결제 완료]
    → stock -= 5, reserved_stock -= 5
    → stock=95, reserved_stock=0, available=95

    [주문 취소/만료]
    → reserved_stock -= 5
    → stock=100, reserved_stock=0, available=100
    ```
  - **동시성 제어 필수**:
    - 여러 사용자가 동시에 같은 상품 주문 시 초과 예약 방지
    - 비관적 락 또는 낙관적 락 적용
    ```sql
    -- 재고 예약
    UPDATE inventory
    SET reserved_stock = reserved_stock + ?
    WHERE product_id = ?
      AND (stock - reserved_stock) >= ?  -- 판매 가능 재고 확인

    -- 영향받은 행이 0이면 재고 부족
    ```

- **`low_stock_threshold` (낮은 재고 기준)**
  - **목적**: 재고 부족 알림 기준값
  - **활용**:
    - 배치 작업으로 `available_stock < low_stock_threshold` 상품 조회
    - 관리자에게 재입고 알림 전송
    - 상품 상태를 `OUT_OF_STOCK`으로 자동 변경

- **`product_id` UNIQUE 제약조건**
  - **목적**: 1 상품 = 1 재고 레코드 보장
  - **멱등성**: 재고 초기화 시 INSERT 대신 UPSERT(INSERT ON DUPLICATE KEY UPDATE) 사용 가능

---


## 참고 문서

- [API Specification](./api-specification.md)
- [Requirements](./requirements.md)
- [User Stories](./user-stories.md)
- [Order Flow](./order-flow.md)
