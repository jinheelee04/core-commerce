# Glossary (용어집)

> E-commerce 프로젝트의 주요 용어 정의. 팀 전체가 동일한 용어를 일관되게 사용하기 위한 참고 문서입니다.

---

## 비즈니스 용어

### 주문/결제

| 용어 | 설명 |
|-----|------|
| **주문 번호** (Order Number) | 고객에게 노출되는 주문 식별 코드. CS 대응 및 문의 시 사용 (예: ORD-20250128-001) |
| **거래 ID** (Transaction ID) | PG사에서 발급하는 결제 건별 고유 ID. 결제 조회/취소 시 필수 |
| **최종 금액** (Final Amount) | 상품 금액 - 할인 금액 = 실제 결제 금액 |
| **최소 주문 금액** (Min Order Amount) | 쿠폰 사용을 위한 최소 구매 금액 조건 |

### 재고

| 용어 | 설명 |
|-----|------|
| **실재고** (Stock) | 창고 보유 실제 재고 수량 |
| **예약재고** (Reserved Stock) | 주문 생성 후 결제 대기 중인 수량 |
| **판매 가능 재고** (Available Stock) | stock - reserved_stock (실제 판매 가능한 수량) |
| **재고 예약** | 주문 생성 시 reserved_stock 증가로 다른 사용자 구매 방지 |
| **재고 확정 차감** | 결제 완료 후 stock 및 reserved_stock 감소 |

### 쿠폰

| 용어 | 설명 |
|-----|------|
| **선착순 발급** | 제한 수량의 쿠폰을 신청 순서대로 발급 |
| **1인 1매 제한** | 동일 사용자가 같은 쿠폰 중복 발급 불가 |
| **정률 할인** (Percentage) | 주문 금액의 일정 비율 할인 (예: 10%) |
| **정액 할인** (Fixed Amount) | 고정된 금액 할인 (예: 5,000원) |

---

## 기술 용어

### API

| 용어 | 설명 |
|-----|------|
| **Endpoint** | API 접근 경로 (예: `/api/v1/products`) |
| **Query Parameter** | URL 필터링/정렬 조건 (예: `?category=electronics&sort=price,asc`) |
| **Path Parameter** | URL 경로 식별자 (예: `/products/{productId}`) |
| **Request Body** | API 요청 시 전송하는 데이터 |
| **페이징** (Pagination) | 대량 데이터를 페이지 단위로 분할 조회 |

### 아키텍처

| 용어 | 설명 |
|-----|------|
| **트랜잭션** (Transaction) | 여러 작업을 하나의 단위로 묶어 모두 성공 또는 모두 실패 처리 |
| **롤백** (Rollback) | 트랜잭션 오류 시 모든 변경을 취소하고 원래 상태로 복구 |
| **동시성 제어** (Concurrency Control) | 여러 사용자의 동시 요청 시 데이터 충돌 방지 |
| **멱등성** (Idempotency) | 동일 요청을 여러 번 보내도 결과가 동일 (예: 결제 중복 클릭 방지) |
| **Mock** | 실제 외부 시스템 대신 사용하는 가상 구현 (테스트용) |
| **PG사** (Payment Gateway) | 실제 결제 처리하는 외부 결제 대행 업체 |

### 성능

| 용어 | 설명 |
|-----|------|
| **TPS** (Transactions Per Second) | 초당 처리 가능한 요청 수 |
| **응답 시간** (Response Time) | 요청 후 응답까지 걸리는 시간 |
| **95th percentile** | 전체 요청 중 95%가 해당 시간 이내 응답 |

---

## HTTP 상태 코드

| 코드 | 이름 | 의미 |
|------|------|------|
| **200** | OK | 요청 성공 |
| **201** | Created | 리소스 생성 성공 |
| **204** | No Content | 성공, 반환 데이터 없음 |
| **400** | Bad Request | 잘못된 요청 (입력 오류) |
| **401** | Unauthorized | 인증 실패 |
| **403** | Forbidden | 권한 없음 |
| **404** | Not Found | 리소스 미존재 |
| **409** | Conflict | 요청이 현재 상태와 충돌 (재고 부족, 중복 등) |
| **500** | Internal Server Error | 서버 오류 |

---

## 데이터베이스

### 기본 개념

| 용어 | 설명 |
|-----|------|
| **Primary Key (PK)** | 테이블의 각 행을 고유하게 식별하는 컬럼 |
| **Foreign Key (FK)** | 다른 테이블의 PK를 참조하는 컬럼 |
| **UNIQUE 제약** | 컬럼 값 중복 방지 |
| **INDEX** | 조회 속도 향상을 위한 색인 |

### 컬럼 약어 및 네이밍

**약어**

| 약어 | 의미 | 예시 |
|------|------|------|
| **qty** | quantity (수량) | `cart_items.quantity` |
| **amt** | amount (금액) | `discount_amount` |
| **desc** | description (설명) | `products.description` |
| **txn** | transaction (거래) | `transaction_id` |

**네이밍 패턴**

| 패턴 | 의미 | 예시 |
|------|------|------|
| **`*_id`** | 식별자 (FK) | `user_id`, `product_id`, `order_id` |
| **`*_at`** | 시점/시각 | `created_at`, `paid_at`, `cancelled_at` |
| **`*_number`** | 사용자 노출 번호 | `order_number` |
| **`*_amount`** | 금액 | `items_total`, `discount_amount`, `final_amount` |
| **`*_quantity`** | 수량 | `total_quantity`, `remaining_quantity` |
| **`is_*`** | Boolean | `is_used`, `is_active` |

**Primary Key 네이밍**

| 방식 | 예시 | 장점 | 단점 |
|------|------|------|------|
| **`id`** | `products.id` | 간결, 테이블이 이미 네임스페이스 제공 | 조인 시 명시성 떨어짐 |
| **`테이블명_id`** | `products.product_id` | 조인 시 명확, FK와 일관성 | 중복감, 장황함 |

> **현재 프로젝트**: `id` 방식 사용 (간결성 우선, 테이블 네임스페이스 활용)
>
> **FK는 `테이블명_id`**: PK는 `id`, FK는 `user_id`, `product_id` 형태로 명확성 유지

### 주요 컬럼 정의

**금액 관련**

| 컬럼 | 의미 | 계산식 |
|------|------|--------|
| `items_total` | 상품 금액 합계 (할인 전) | sum(unit_price × quantity) |
| `discount_amount` | 할인 금액 | 쿠폰 할인 계산 결과 |
| `final_amount` | 최종 결제 금액 | items_total - discount_amount |

**재고 관련**

| 컬럼 | 의미 |
|------|------|
| `stock` | 실제 보유 재고 |
| `reserved_stock` | 예약된 재고 (결제 대기) |
| `available_stock` | 판매 가능 재고 (stock - reserved_stock) |
| `low_stock_threshold` | 재고 부족 알림 기준 |

---

## 주문 상태

| 상태 | 의미 | 다음 상태 |
|------|------|----------|
| **PENDING** | 결제 대기 | PAID, CANCELLED |
| **PAID** | 결제 완료 | CONFIRMED, CANCELLED |
| **CONFIRMED** | 주문 확정 (배송 준비) | - |
| **CANCELLED** | 주문 취소 | - |

---

## 에러 코드

| 코드 | 의미 |
|------|------|
| `PRODUCT_OUT_OF_STOCK` | 품절 상품 |
| `INSUFFICIENT_STOCK` | 재고 부족 |
| `COUPON_ALREADY_ISSUED` | 쿠폰 중복 발급 |
| `COUPON_OUT_OF_STOCK` | 쿠폰 소진 |
| `COUPON_ALREADY_USED` | 쿠폰 이미 사용 |
| `ORDER_ALREADY_PAID` | 주문 이미 결제됨 |
| `PAYMENT_AMOUNT_MISMATCH` | 결제 금액 불일치 |
| `VALIDATION_ERROR` | 입력 검증 실패 |

---

## 참고 문서

- [API Specification](./api-specification.md)
- [Requirements](./requirements.md)
- [User Stories](./user-stories.md)
- [Order Flow](./order-flow.md)
- [Data Models](./data-models.md)