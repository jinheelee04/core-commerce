# 📖 User Stories - E-commerce Service

> **작성 형식**: As a [role], I want [feature], so that [benefit]
> 각 스토리는 인수 조건(Acceptance Criteria)을 포함합니다.

---

## 1️⃣ 상품 관리 (Product Management)

### US-P-01: 상품 목록 조회
**As a** Customer
**I want** to browse products with filtering and sorting options
**So that** I can easily find products that match my preferences

**Acceptance Criteria:**
- [ ] 카테고리별 필터링 가능 (`?category=electronics`)
- [ ] 가격 범위 필터링 가능 (`?minPrice=1000&maxPrice=50000`)
- [ ] 키워드 검색 가능 (`?keyword=laptop`)
- [ ] 정렬 옵션 지원 (`?sort=price,asc` / `createdAt,desc` / `popular`)
- [ ] 페이징 지원 (`?page=0&size=20`)
- [ ] 품절 상품도 목록에 표시되지만 구분 가능
- [ ] 응답 시간 200ms 이하

**Example Response:**
```json
{
  "data": [
    {
      "productId": 1,
      "name": "MacBook Pro",
      "price": 2500000,
      "category": "ELECTRONICS",
      "status": "AVAILABLE",
      "stockQuantity": 10
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

### US-P-02: 상품 상세 조회
**As a** Customer
**I want** to view detailed product information
**So that** I can make an informed purchase decision

**Acceptance Criteria:**
- [ ] 상품 ID로 상세 정보 조회 가능
- [ ] 상품명, 가격, 설명, 카테고리 표시
- [ ] 현재 재고 수량 표시 (`availableStock = stock - reservedStock`)
- [ ] 존재하지 않는 상품 조회 시 404 에러 반환
- [ ] 품절 상품도 조회 가능하되 상태 명시

**Example Response:**
```json
{
  "productId": 1,
  "name": "MacBook Pro",
  "description": "Apple M3 chip, 16GB RAM",
  "price": 2500000,
  "category": "ELECTRONICS",
  "status": "AVAILABLE",
  "stock": 10,
  "reservedStock": 2,
  "availableStock": 8,
  "createdAt": "2025-01-15T10:00:00Z"
}
```
---

## 2️⃣ 장바구니 (Shopping Cart)

### US-C-01: 장바구니에 상품 담기
**As a** Customer
**I want** to add products to my shopping cart
**So that** I can purchase multiple items together later

**Acceptance Criteria:**
- [ ] 상품 ID와 수량을 지정하여 장바구니에 추가
- [ ] 동일 상품 추가 시 수량 증가
- [ ] 품절 상품 추가 시 400 에러 반환
- [ ] 수량은 1 이상이어야 함

**Example Request:**
```json
{
  "productId": 1,
  "quantity": 2
}
```

---

### US-C-02: 장바구니 조회
**As a** Customer
**I want** to view all items in my cart
**So that** I can review what I plan to purchase

**Acceptance Criteria:**
- [ ] 사용자별 장바구니 항목 전체 조회
- [ ] 각 항목의 상품 정보(이름, 가격), 수량, 소계 표시
- [ ] 총 금액 계산 포함
- [ ] 빈 장바구니도 조회 가능 (빈 배열 반환)

**Example Response:**
```json
{
  "cartId": 1,
  "userId": 123,
  "items": [
    {
      "cartItemId": 1,
      "productId": 1,
      "productName": "MacBook Pro",
      "price": 2500000,
      "quantity": 2,
      "subtotal": 5000000
    }
  ],
  "totalAmount": 5000000
}
```

---

### US-C-03: 장바구니 수량 변경
**As a** Customer
**I want** to update the quantity of items in my cart
**So that** I can adjust my order before purchasing

**Acceptance Criteria:**
- [ ] 장바구니 항목 ID로 수량 변경
- [ ] 수량은 1 이상이어야 함
- [ ] 재고보다 많은 수량 변경 시 409 에러 반환
- [ ] 존재하지 않는 항목 수정 시 404 에러 반환

---

### US-C-04: 장바구니에서 상품 삭제
**As a** Customer
**I want** to remove individual items from my cart
**So that** I can exclude items I no longer want

**Acceptance Criteria:**
- [ ] 장바구니 항목 ID로 삭제
- [ ] 존재하지 않는 항목 삭제 시 404 에러 반환
- [ ] 삭제 후 장바구니 재조회 시 해당 항목 미표시

---

### US-C-05: 장바구니 비우기
**As a** Customer
**I want** to clear all items from my cart at once
**So that** I can start fresh without deleting items one by one

**Acceptance Criteria:**
- [ ] 사용자의 모든 장바구니 항목 일괄 삭제
- [ ] 빈 장바구니 비우기 시도도 성공 처리 (204 No Content)
- [ ] 비우기 후 조회 시 빈 배열 반환

---

## 3️⃣ 주문 관리 (Order Management)

### US-O-01: 주문 생성
**As a** Customer
**I want** to create an order from my cart items
**So that** I can proceed to payment

**Acceptance Criteria:**
- [ ] 장바구니의 선택된 항목으로 주문 생성
- [ ] 주문 생성 시 재고 예약 (`reservedStock` 증가)
- [ ] 주문 상태는 `PENDING`으로 시작
- [ ] 쿠폰 적용 가능 (선택사항)
- [ ] 쿠폰 적용 시 할인 금액 자동 계산
- [ ] 재고 부족 시 409 에러 반환 ("재고가 부족합니다")
- [ ] 빈 장바구니로 주문 시도 시 400 에러 반환
- [ ] 주문 생성 후 주문 ID, 총 금액, 할인 금액, 최종 금액 반환
- [ ] 트랜잭션 내에서 Order + OrderItem + 재고 예약 원자적 처리

**Example Request (쿠폰 미사용):**
```json
{
  "userId": 123,
  "cartItemIds": [1, 2, 3],
  "deliveryAddress": "서울시 강남구..."
}
```

**Example Request (쿠폰 사용):**
```json
{
  "userId": 123,
  "cartItemIds": [1, 2, 3],
  "couponId": "coupon_abc123",
  "deliveryAddress": "서울시 강남구..."
}
```

**Example Response (쿠폰 사용):**
```json
{
  "orderId": 456,
  "userId": 123,
  "status": "PENDING",
  "pricing": {
    "itemsTotal": 5000000,
    "discountAmount": 500000,
    "finalAmount": 4500000
  },
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "MacBook Pro",
      "quantity": 2,
      "price": 2500000
    }
  ],
  "coupon": {
    "couponId": "coupon_abc123",
    "name": "10% 할인 쿠폰",
    "discountAmount": 500000
  },
  "createdAt": "2025-01-15T10:30:00Z"
}
```

---

### US-O-02: 주문 상세 조회
**As a** Customer
**I want** to view details of a specific order
**So that** I can check what I ordered and its current status

**Acceptance Criteria:**
- [ ] 주문 ID로 상세 정보 조회
- [ ] 주문 상품 목록, 수량, 가격, 총 금액 표시
- [ ] 현재 주문 상태 표시 (`PENDING`, `PAID`, `CONFIRMED`, `CANCELLED`)
- [ ] 결제 정보 포함 (있는 경우)
- [ ] 존재하지 않는 주문 조회 시 404 에러 반환
- [ ] 다른 사용자의 주문 조회 시 403 에러 반환

---

### US-O-03: 주문 이력 조회
**As a** Customer
**I want** to view all my past orders
**So that** I can track my purchase history

**Acceptance Criteria:**
- [ ] 사용자별 주문 내역 페이징 조회
- [ ] 상태별 필터링 가능 (`?status=PAID`)
- [ ] 최신 주문이 먼저 표시 (기본 정렬: `createdAt,desc`)
- [ ] 각 주문의 요약 정보 표시 (주문번호, 총 금액, 상태, 날짜)
- [ ] 빈 결과도 정상 응답 (빈 배열)

**Example Response:**
```json
{
  "data": [
    {
      "orderId": 456,
      "totalAmount": 5000000,
      "status": "PAID",
      "itemCount": 2,
      "createdAt": "2025-01-15T10:30:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 15,
    "totalPages": 1
  }
}
```

---

### US-O-04: 주문 취소
**As a** Customer
**I want** to cancel my pending order
**So that** I can avoid payment if I change my mind

**Acceptance Criteria:**
- [ ] `PENDING` 상태의 주문만 취소 가능
- [ ] 주문 취소 시 재고 예약 해제 (`reservedStock` 감소)
- [ ] 주문 상태 `CANCELLED`로 변경
- [ ] 이미 결제된 주문(`PAID`) 취소 시 400 에러 반환
- [ ] 이미 취소된 주문 재취소 시도 시 400 에러 반환
- [ ] 존재하지 않는 주문 취소 시 404 에러 반환

---

## 4️⃣ 결제 시스템 (Payment)

### US-PMT-01: 결제 요청
**As a** Customer
**I want** to make a payment for my order
**So that** I can complete my purchase

**Acceptance Criteria:**
- [ ] 주문 ID로 결제 요청
- [ ] Mock Payment API 호출 (실제 결제 시뮬레이션)
- [ ] 쿠폰 코드 입력 시 할인 적용 (선택사항)
- [ ] `PENDING` 상태의 주문만 결제 가능
- [ ] 이미 결제된 주문 재결제 시도 시 400 에러 반환
- [ ] 결제 금액과 주문 총액 불일치 시 400 에러 반환

**Example Request:**
```json
{
  "orderId": 456,
  "paymentMethod": "CARD",
  "amount": 5000000,
  "couponCode": "WELCOME2024"
}
```

---

### US-PMT-02: 결제 성공 처리
**As a** System
**I want** to process successful payments automatically
**So that** orders are confirmed and inventory is updated

**Acceptance Criteria:**
- [ ] 결제 성공 시 주문 상태 `PAID`로 변경
- [ ] 재고 확정 차감 (`stock` 감소, `reservedStock` 감소)
- [ ] 쿠폰 사용 처리 (사용된 쿠폰 `isUsed=true`)
- [ ] Payment 레코드 생성 (status=`SUCCESS`)
- [ ] 트랜잭션 내에서 Payment + Order + Inventory + Coupon 원자적 처리
- [ ] 처리 실패 시 전체 롤백

---

### US-PMT-03: 결제 실패 처리
**As a** System
**I want** to handle payment failures automatically
**So that** reserved inventory is released

**Acceptance Criteria:**
- [ ] 결제 실패 시 주문 상태 `CANCELLED`로 변경
- [ ] 재고 예약 해제 (`reservedStock` 감소)
- [ ] Payment 레코드 생성 (status=`FAILED`)
- [ ] 쿠폰 사용 상태 유지 (미사용 상태로 복구)
- [ ] 실패 사유 기록

---

### US-PMT-04: 결제 상태 조회
**As a** Customer
**I want** to check the status of my payment
**So that** I can confirm whether my payment was successful

**Acceptance Criteria:**
- [ ] 주문 ID 또는 결제 ID로 조회
- [ ] 결제 상태 표시 (`PENDING`, `SUCCESS`, `FAILED`)
- [ ] 결제 금액, 결제 방법, 결제 일시 표시
- [ ] 존재하지 않는 결제 조회 시 404 에러 반환

**Example Response:**
```json
{
  "paymentId": 789,
  "orderId": 456,
  "amount": 5000000,
  "discountAmount": 0,
  "finalAmount": 5000000,
  "paymentMethod": "CARD",
  "status": "SUCCESS",
  "paidAt": "2025-01-15T10:35:00Z"
}
```

---

## 5️⃣  쿠폰 시스템 (Coupon)

### US-CPN-01: 쿠폰 발급 (선착순)
**As a** Customer
**I want** to claim available coupons on a first-come-first-served basis
**So that** I can receive discounts

**Acceptance Criteria:**
- [ ] 쿠폰 ID로 발급 요청
- [ ] 1인 1매 제한 (중복 발급 시 409 에러)
- [ ] 선착순으로 수량 제한 (수량 소진 시 409 에러)
- [ ] 동시성 제어 
- [ ] DB에 UserCoupon 레코드 생성
- [ ] 만료된 쿠폰 발급 시도 시 400 에러 반환

---

### US-CPN-02: 보유 쿠폰 조회
**As a** Customer
**I want** to view all my coupons
**So that** I can see what discounts I can use

**Acceptance Criteria:**
- [ ] 사용자별 쿠폰 목록 조회
- [ ] 사용 여부 필터링 가능 (`?isUsed=false`)
- [ ] 만료된 쿠폰도 조회 가능하되 구분 표시
- [ ] 각 쿠폰의 할인 정보, 유효기간 표시

**Example Response:**
```json
{
  "data": [
    {
      "userCouponId": 1,
      "couponId": 10,
      "code": "WELCOME2024",
      "name": "신규 가입 환영 쿠폰",
      "discountType": "FIXED_AMOUNT",
      "discountValue": 10000,
      "isUsed": false,
      "expiryDate": "2025-12-31T23:59:59Z",
      "issuedAt": "2025-01-15T09:00:00Z"
    }
  ]
}
```

---

### US-CPN-03: 쿠폰 사용
**As a** Customer
**I want** to apply a coupon code during payment
**So that** I can receive a discount on my order

**Acceptance Criteria:**
- [ ] 결제 시 쿠폰 코드 입력
- [ ] 유효한 쿠폰인지 검증 (존재 여부, 소유 여부, 만료 여부)
- [ ] 이미 사용된 쿠폰 재사용 시 400 에러 반환
- [ ] 할인 금액 계산 및 적용
- [ ] 쿠폰 사용 처리 (`isUsed=true`, `usedAt` 기록)
- [ ] 최소 주문 금액 미충족 시 400 에러 반환

---

## 🎯 E2E 시나리오

### US-E2E-01: 완전한 구매 플로우
**As a** Customer
**I want** to complete a full purchase from browsing to payment
**So that** I can receive my products

**End-to-End Flow:**
1. 상품 목록 조회 (US-P-01)
2. 상품 상세 확인 (US-P-02)
3. 장바구니에 상품 추가 (US-C-01)
4. 장바구니 확인 및 수량 조정 (US-C-02, US-C-03)
5. 쿠폰 발급 (US-CPN-01) - 선택사항
6. 주문 생성 + 쿠폰 적용 (US-O-01) → 재고 예약
7. 결제 진행 (US-PMT-01)
8. 결제 성공 (US-PMT-02) → 재고 확정 차감 + 쿠폰 사용 처리 (US-CPN-03)
9. 주문 확인 (US-O-02)

**Acceptance Criteria:**
- [ ] 전체 플로우가 3분 이내에 완료
- [ ] 각 단계에서 오류 발생 시 이전 단계 롤백
- [ ] 재고 부족 시 주문 생성 단계에서 실패
- [ ] 결제 실패 시 재고 예약 자동 해제
- [ ] 쿠폰 사용 시 할인이 정확하게 적용

---

## 📝 요약

- **총 User Stories**: 21개
- **주요 역할**: Customer, System
- **핵심 도메인**: Product, Cart, Order, Payment, Inventory, Coupon

