# 🧩 E-commerce Service Requirements Specification (core-commerce)

> **과제 목표**  
> 상품, 장바구니, 주문, 결제, 쿠폰, 재고 등  
> 이커머스 서비스의 핵심 기능을 중심으로 RESTful API를 설계한다.  
> Mock 결제 및 Redis 기반 쿠폰 시스템을 활용하여 결제/동시성 로직을 학습하고,

---

## 1️⃣ 사용자 (User)

| ID | 기능명 | 상세 내용 |
| --- | --- | --- |
| U-01 | 주문 내역 조회 | 사용자가 주문한 내역을 상태별로 확인 |
| U-02 | 쿠폰 조회 | 보유 쿠폰 목록 및 사용 여부 확인 |
| U-03 | 결제 내역 조회 | 주문별 결제 상태 확인 |
| U-04 | 알림 확인 | 결제 성공/취소/쿠폰 발급 등 알림 조회 |

---

## 2️⃣ 상품 관리 (Product)

| ID   | 기능명 | 상세 내용 |
|------| --- | --- |
| P-01 | 상품 목록 조회 | 카테고리, 브랜드, 가격대, 정렬 기준으로 조회 |
| P-02 | 상품 상세 조회 | 상품 정보, 재고 수량, 리뷰 요약 등 상세 정보 표시 |
| P-03 | 상품 상태 변경 | 판매중 ↔ 품절 상태 전환 |


---

## 3️⃣ 장바구니 (Cart)

| ID | 기능명 | 상세 내용 |
| --- | --- | --- |
| C-01 | 장바구니 담기 | 사용자가 상품을 장바구니에 추가 |
| C-02 | 장바구니 조회 | 담긴 상품 목록 및 수량 확인 |
| C-03 | 수량 변경 | 장바구니 내 상품 수량 수정 |
| C-04 | 상품 삭제 | 장바구니에서 개별 상품 제거 |
| C-05 | 장바구니 비우기 | 모든 상품 한 번에 삭제 |

---

## 4️⃣ 주문 관리 (Order)

| ID | 기능명 | 상세 내용 |
| --- | --- | --- |
| O-01 | 주문 생성 | 장바구니에서 선택한 상품으로 주문 생성 |
| O-02 | 주문 상세 조회 | 주문 ID 기준으로 상품 목록, 결제 금액, 상태 조회 |
| O-03 | 주문 상태 변경 | 결제 대기 → 결제 완료 → 주문 확정 등 상태 변경 |
| O-04 | 주문 취소 | 결제 실패 또는 사용자 취소 시 주문 취소 |
| O-05 | 주문 이력 조회 | 사용자별 주문 내역 페이징 조회 |

---

## 5️⃣ 결제 시스템 (Payment)

| ID | 기능명 | 상세 내용 |
| --- | --- | --- |
| PAY-01 | 결제 요청 (Mock) | 주문 생성 후 Mock Payment API 호출 (결제 시뮬레이션) |
| PAY-02 | 결제 성공 처리 | 결제 완료 시 주문 상태 `PAID`, 재고 확정 차감, 쿠폰 적용 |
| PAY-03 | 결제 실패 처리 | 실패 시 주문 상태 `CANCELLED`, 재고 예약 해제 |
| PAY-04 | 결제 상태 조회 | 결제 상태(대기, 성공, 실패) 조회 |

---

## 6️⃣ 재고 관리 (Inventory)

| ID | 기능명 | 상세 내용 |
| --- | --- | --- |
| INV-01 | 재고 예약 | 주문 생성 시 `reserved_stock` 증가 |
| INV-02 | 재고 확정 차감 | 결제 성공 시 `stock` 및 `reserved_stock` 감소 |
| INV-03 | 재고 예약 해제 | 결제 실패 또는 주문 취소 시 `reserved_stock` 감소 |
| INV-04 | 재고 조회 | 상품별 현재 재고 수량 및 예약 재고 조회 |

---

## 7️⃣ 쿠폰 시스템 (Coupon)

| ID | 기능명 | 상세 내용 |
| --- | --- | --- |
| CO-01 | 쿠폰 생성 | 기본 쿠폰 정보(수량, 할인율, 기간) 등록 |
| CO-02 | 쿠폰 발급 | 사용자 선착순 발급 (Redis 기반, 1인 1매 제한) |
| CO-03 | 쿠폰 조회 | 사용자 보유 쿠폰 목록 조회 |
| CO-04 | 쿠폰 사용 | 결제 시 쿠폰 코드 입력 후 Mock 할인 처리 |
| CO-05 | 쿠폰 만료 | 유효기간 만료 시 자동 비활성화 |

---

## 🧱 데이터 모델 요약

| 엔터티 | 주요 필드 | 설명 |
|---------|------------|------|
| **User** | user_id, name | 사용자 기본정보 |
| **Product** | product_id, name, price, category | 상품 기본 정보 |
| **Cart** | cart_id, user_id | 사용자별 장바구니 |
| **CartItem** | cart_item_id, cart_id, product_id, quantity | 장바구니 상품 정보 |
| **Order** | order_id, user_id, total_price, status | 주문 정보 |
| **OrderItem** | order_item_id, order_id, product_id, quantity, price | 주문별 상품 구성 |
| **Payment** | payment_id, order_id, amount, status | 결제(Mock) |
| **Coupon** | coupon_id, name, discount_value, expiry_date | 쿠폰 정의 |
| **UserCoupon** | user_coupon_id, coupon_id, user_id, is_used | 사용자 쿠폰 내역 |
| **Inventory** | product_id, stock, reserved_stock | 재고 관리 (Product와 1:1 관계) |

---

## ⚙️ 비기능 및 시스템 요구사항

### 1️⃣ 성능 및 동시성
| ID | 항목 | 요구사항 |
| --- | --- | --- |
| NFR-01 | API 응답시간 | 평균 200ms 이하, 95th percentile 500ms 이하 |
| NFR-02 | 동시 요청 처리 | 최소 100 TPS 처리 가능 |
| NFR-03 | 재고 동시성 제어 | 기본적인 동시성 이슈 방지 |
| NFR-04 | 쿠폰 동시성 제어 | 기본적인 선착순 발급 보장 |
| NFR-05 | 트랜잭션 격리 수준 | READ_COMMITTED 이상 |

### 2️⃣ 재고 예약 시스템
| ID | 기능명 | 상세 내용 |
| --- | --- | --- |
| RS-01 | 재고 예약 | 주문 생성 시 `reserved_stock` 증가, `available_stock` 계산 |
| RS-02 | 재고 확정 차감 | 결제 성공 시 `reserved_stock` 감소, `stock` 감소 |
| RS-03 | 재고 예약 해제 | 결제 실패/취소 시 `reserved_stock` 감소 |
| RS-04 | 재고 부족 검증 | `stock - reserved_stock < 주문수량` 시 주문 실패 |
| RS-05 | 예약 타임아웃 | 결제 대기 15분 초과 시 예약 자동 해제 |

### 3️⃣ API 설계 표준
| 항목 | 규칙 |
| --- | --- |
| 페이징 | `page` (0-based), `size` (20), `sort` |
| 정렬 옵션 | 상품: `price,asc`, `createdAt,desc`, `popular` 등 |
| 필터링 | `category`, `minPrice`, `maxPrice`, `keyword` |
| HTTP 상태코드 | 200, 201, 400, 404, 409, 500 |
| 에러 응답 포맷 | `{ "error": "ERROR_CODE", "message": "상세 메시지" }` |
| 성공 응답 포맷 | `{ "data": {...}, "meta": { "page": 0, "size": 20 } }` |

### 4️⃣ 주문 상태 전이 규칙
| 현재 상태 | 다음 상태 | 조건 |
| --- | --- | --- |
| `PENDING` | `PAID` | 결제 성공 |
| `PENDING` | `CANCELLED` | 결제 실패/타임아웃 |
| `PAID` | `CONFIRMED` | 주문 확정 |
| `PAID` | `CANCELLED` | 결제 취소 |
| `CANCELLED` | - | 최종 상태 |

### 5️⃣ 예외 상황 처리
| 상황 | 처리 방식 |
| --- | --- |
| 재고 부족 | 409 Conflict, 주문 실패 |
| 쿠폰 수량 초과 | 409 Conflict, "쿠폰이 모두 소진되었습니다" |
| 쿠폰 중복 사용 | 400 Bad Request, "이미 사용된 쿠폰입니다" |
| 쿠폰 중복 발급 | 409 Conflict |
| 이미 취소된 주문 | 400 Bad Request |
| 결제 타임아웃 | 15분 초과 시 자동 취소 |
| 상품 미존재 | 404 Not Found |
| 장바구니 항목 오류 | 400 Bad Request |

### 6️⃣ 트랜잭션 경계
| 시나리오 | 트랜잭션 범위 |
| --- | --- |
| 주문 생성 | Order + OrderItem + 재고 예약 |
| 결제 성공 | Payment + Order + 재고 차감 + 쿠폰 사용 |
| 결제 실패 | Payment + Order 취소 + 재고 해제 |
| 장바구니 담기 | CartItem 생성/수정 |
| 쿠폰 발급 | UserCoupon 생성 + Redis 카운트 감소 |

---

## 🔁 주요 프로세스 플로우

### 주문 생성
1️⃣ 장바구니 검증  
2️⃣ 재고 확인 (`stock - reserved_stock` 체크)  
3️⃣ 재고 예약 (`reserved_stock` 증가)  
4️⃣ 주문 생성 (status=`PENDING`)  
5️⃣ 주문 ID, 총 금액, 상태 반환

### 결제 처리
1️⃣ 결제 요청(Mock Payment API)  
2️⃣ 성공 → 주문 `PAID`, 재고 확정 차감, 쿠폰 사용  
3️⃣ 실패 → 주문 `CANCELLED`, 예약 해제

### 쿠폰 발급
1️⃣ Redis로 중복 발급 확인 (`SISMEMBER user:coupon:{couponId} {userId}`)
2️⃣ 수량 체크 (`GET coupon:{id}:count`)
3️⃣ MULTI/EXEC 트랜잭션으로 원자적 발급
   - `SADD user:coupon:{couponId} {userId}`
   - `DECR coupon:{id}:count`
4️⃣ DB 저장 → UserCoupon 생성
5️⃣ 중복 발급 또는 수량 초과 시 409 반환

---

## 🔄 전체 서비스 흐름

```plaintext
[상품 목록 조회]
   ↓
[장바구니 담기 / 수정 / 삭제]
   ↓
[주문 생성 (장바구니 기반)]
   ↓
[재고 예약 (reserved_stock 증가)]
   ↓
[결제 요청 (Mock)]
   ↓
 ├─ 성공 → 재고 차감, 쿠폰 할인, 주문 상태 PAID
 └─ 실패 → 예약 해제, 주문 상태 CANCELLED

```

---

## 📘 확장 고려사항
- 배송(Delivery), 정산(Settlement) 도메인으로 확장 가능
- 리뷰 시스템 연동 시 OrderItem 참조 활용
- 실제 PG(Payment Gateway) 연동 시 Mock API 대체 가능
- Redis 분산 락으로 다중 서버 환경 동시성 제어 가능