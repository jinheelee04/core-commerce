# 🔌 E-commerce API Specification

> **RESTful API 명세서**
>
> Version: v1.0
> Base URL: `/api/v1`
> 기반 문서: user-stories.md, requirements.md

---

## 목차

1. [공통 사항](#공통-사항)
2. [상품 API](#상품-api)
3. [장바구니 API](#장바구니-api)
4. [주문 API](#주문-api)
5. [결제 API](#결제-api)
6. [쿠폰 API](#쿠폰-api)
7. [에러 응답](#에러-응답)

---

## 공통 사항

### 공통 헤더

```http
Content-Type: application/json
Accept: application/json
X-User-Id: 123
```

### 페이징 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|-------|------|
| `page` | integer | 0 | 페이지 번호 (0-based) |
| `size` | integer | 20 | 페이지 크기 (최대 100) |
| `sort` | string | - | 정렬 기준 (예: `price,asc`, `createdAt,desc`) |

### 공통 응답 형식

**성공 응답 (단일 객체)**
```json
{
  "data": {
    "id": 1,
    "name": "..."
  }
}
```

**성공 응답 (목록 + 페이징)**
```json
{
  "data": [ ... ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

## 상품 API

### 1. 상품 목록 조회

**Endpoint**: `GET /products`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|-----|------|
| `category` | string | N | 카테고리 필터 (예: `electronics`) |
| `keyword` | string | N | 검색 키워드 (상품명, 설명) |
| `minPrice` | integer | N | 최소 가격 |
| `maxPrice` | integer | N | 최대 가격 |
| `sort` | string | N | 정렬 (`price,asc`, `createdAt,desc`, `popular`) |
| `page` | integer | N | 페이지 번호 (기본: 0) |
| `size` | integer | N | 페이지 크기 (기본: 20) |

**Response 200 OK**
```json
{
  "data": [
    {
      "productId": 1,
      "name": "MacBook Pro",
      "description": "Apple M3 chip, 16GB RAM",
      "price": 2500000,
      "category": "ELECTRONICS",
      "brand": "Apple",
      "imageUrl": "https://cdn.example.com/products/1.jpg",
      "status": "AVAILABLE",
      "availableStock": 10,
      "createdAt": "2025-01-15T10:00:00Z"
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

### 2. 상품 상세 조회

**Endpoint**: `GET /products/{productId}`

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `productId` | integer | 상품 ID |

**Response 200 OK**
```json
{
  "data": {
    "productId": 1,
    "name": "MacBook Pro",
    "description": "Apple M3 chip, 16GB RAM",
    "price": 2500000,
    "category": "ELECTRONICS",
    "brand": "Apple",
    "imageUrl": "https://cdn.example.com/products/1.jpg",
    "status": "AVAILABLE",
    "stock": 10,
    "reservedStock": 2,
    "availableStock": 8,
    "createdAt": "2025-01-15T10:00:00Z",
    "updatedAt": "2025-01-20T14:00:00Z"
  }
}
```

**Response 404 Not Found**
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다"
}
```

---

## 장바구니 API

### 1. 장바구니 조회

**Endpoint**: `GET /carts/me`

**Response 200 OK**
```json
{
  "data": {
    "cartId": 1,
    "userId": 123,
    "items": [
      {
        "cartItemId": 1,
        "productId": 1,
        "productName": "MacBook Pro",
        "productImageUrl": "https://cdn.example.com/products/1.jpg",
        "price": 2500000,
        "quantity": 2,
        "subtotal": 5000000,
        "availableStock": 10,
        "createdAt": "2025-01-25T09:00:00Z"
      }
    ],
    "totalQuantity": 2,
    "totalAmount": 5000000,
    "updatedAt": "2025-01-25T09:00:00Z"
  }
}
```

**빈 장바구니**
```json
{
  "data": {
    "cartId": 1,
    "userId": 123,
    "items": [],
    "totalQuantity": 0,
    "totalAmount": 0
  }
}
```

---

### 2. 장바구니에 상품 담기

**Endpoint**: `POST /carts/items`

**Request Body**
```json
{
  "productId": 1,
  "quantity": 2
}
```

**Validation Rules**
- `productId`: 필수, 양의 정수
- `quantity`: 필수, 1 이상

**Response 201 Created**
```json
{
  "data": {
    "cartItemId": 1,
    "productId": 1,
    "productName": "MacBook Pro",
    "quantity": 2,
    "subtotal": 5000000,
    "createdAt": "2025-01-25T09:00:00Z"
  }
}
```

**Response 400 Bad Request (품절 상품)**
```json
{
  "code": "PRODUCT_OUT_OF_STOCK",
  "message": "품절된 상품입니다"
}
```

---

### 3. 장바구니 수량 변경

**Endpoint**: `PATCH /carts/items/{cartItemId}`

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `cartItemId` | integer | 장바구니 항목 ID |

**Request Body**
```json
{
  "quantity": 3
}
```
**Validation Rules**
- `quantity`: 필수, 1 이상

**Response 200 OK**
```json
{
  "data": {
    "cartItemId": 1,
    "productId": 1,
    "quantity": 3,
    "subtotal": 7500000,
    "updatedAt": "2025-01-25T09:30:00Z"
  }
}
```

**Response 404 Not Found**
```json
{
  "code": "CART_ITEM_NOT_FOUND",
  "message": "장바구니 항목을 찾을 수 없습니다"
}
```

---

### 4. 장바구니 항목 삭제

**Endpoint**: `DELETE /carts/items/{cartItemId}`

**Response 204 No Content**

**Response 404 Not Found**
```json
{
  "code": "CART_ITEM_NOT_FOUND",
  "message": "장바구니 항목을 찾을 수 없습니다"
}
```

---

### 5. 장바구니 비우기

**Endpoint**: `DELETE /carts/items`

**Response 204 No Content**

---

## 주문 API

### 1. 주문 생성

**Endpoint**: `POST /orders`

**Request Body (쿠폰 미사용)**
```json
{
  "cartItemIds": [1, 2, 3],
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "deliveryMemo": "문 앞에 놔주세요"
}
```

**Request Body (쿠폰 사용)**
```json
{
  "cartItemIds": [1, 2, 3],
  "couponId": 10,
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "deliveryMemo": "문 앞에 놔주세요"
}
```

**Validation Rules**
- `cartItemIds`: 필수, 1개 이상의 장바구니 항목 ID
- `couponId`: 선택, 사용할 쿠폰 ID
- `deliveryAddress`: 필수, 배송 주소
- `deliveryMemo`: 선택, 배송 메모

**Note**: 사용자 정보는 인증 헤더(`X-User-Id`)에서 추출

**Response 201 Created**
```json
{
  "data": {
    "orderId": 456,
    "userId": 123,
    "orderNumber": "ORD-20250128-001",
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
        "price": 2500000,
        "subtotal": 5000000
      }
    ],
    "coupon": {
      "couponId": 10,
      "name": "10% 할인 쿠폰",
      "discountAmount": 500000
    },
    "deliveryAddress": "서울시 강남구 테헤란로 123",
    "createdAt": "2025-01-28T10:00:00Z",
    "expiresAt": "2025-01-28T10:15:00Z"
  }
}
```

**Response 400 Bad Request (빈 장바구니)**
```json
{
  "code": "EMPTY_CART",
  "message": "장바구니가 비어있습니다"
}
```

**Response 409 Conflict (재고 부족)**
```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "재고가 부족합니다",
  "details": {
    "insufficientItems": [
      {
        "productId": 1,
        "productName": "MacBook Pro",
        "requestedQuantity": 10,
        "availableStock": 5
      }
    ]
  }
}
```

**Response 400 Bad Request (쿠폰 만료)**
```json
{
  "code": "COUPON_EXPIRED",
  "message": "만료된 쿠폰입니다",
  "details": {
    "couponId": 10,
    "expiresAt": "2025-01-27T23:59:59Z"
  }
}
```

**Response 400 Bad Request (쿠폰 이미 사용됨)**
```json
{
  "code": "COUPON_ALREADY_USED",
  "message": "이미 사용된 쿠폰입니다",
  "details": {
    "couponId": 10,
    "usedAt": "2025-01-20T10:00:00Z"
  }
}
```

**Response 400 Bad Request (최소 주문 금액 미달)**
```json
{
  "code": "COUPON_MIN_ORDER_AMOUNT_NOT_MET",
  "message": "쿠폰 사용을 위한 최소 주문 금액을 충족하지 못했습니다",
  "details": {
    "couponId": 10,
    "minOrderAmount": 50000,
    "currentAmount": 30000
  }
}
```

**Response 404 Not Found (쿠폰 없음)**
```json
{
  "code": "COUPON_NOT_FOUND",
  "message": "쿠폰을 찾을 수 없거나 사용자에게 발급되지 않은 쿠폰입니다"
}
```

---

### 2. 주문 상세 조회

**Endpoint**: `GET /orders/{orderId}`

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `orderId` | integer | 주문 ID |

**Response 200 OK**
```json
{
  "data": {
    "orderId": 456,
    "userId": 123,
    "orderNumber": "ORD-20250128-001",
    "status": "PAID",
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
        "productImageUrl": "https://cdn.example.com/products/1.jpg",
        "quantity": 2,
        "price": 2500000,
        "subtotal": 5000000
      }
    ],
    "payment": {
      "paymentId": 789,
      "method": "CARD",
      "status": "SUCCESS",
      "paidAt": "2025-01-28T10:05:00Z"
    },
    "coupon": {
      "couponId": 10,
      "name": "10% 할인 쿠폰",
      "discountAmount": 500000
    },
    "deliveryAddress": "서울시 강남구 테헤란로 123",
    "createdAt": "2025-01-28T10:00:00Z",
    "updatedAt": "2025-01-28T10:05:00Z"
  }
}
```

**Response 404 Not Found**
```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다"
}
```

**Response 403 Forbidden**
```json
{
  "code": "FORBIDDEN",
  "message": "다른 사용자의 주문에 접근할 수 없습니다"
}
```

---

### 3. 주문 이력 조회

**Endpoint**: `GET /orders`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|-----|------|
| `status` | string | N | 주문 상태 필터 (`PENDING`, `PAID`, `CONFIRMED`, `CANCELLED`) |
| `startsAt` | string | N | 시작 날짜 (ISO 8601) |
| `endsAt` | string | N | 종료 날짜 (ISO 8601) |
| `page` | integer | N | 페이지 번호 |
| `size` | integer | N | 페이지 크기 |
| `sort` | string | N | 정렬 기준 (기본: `createdAt,desc`) |

**Response 200 OK**
```json
{
  "data": [
    {
      "orderId": 456,
      "orderNumber": "ORD-20250128-001",
      "status": "PAID",
      "itemCount": 2,
      "totalAmount": 4500000,
      "createdAt": "2025-01-28T10:00:00Z"
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

### 4. 주문 취소

**Endpoint**: `PATCH /orders/{orderId}`

**Request Body**
```json
{
  "status": "CANCELLED",
  "cancelReason": "단순 변심"
}
```

**Response 200 OK**
```json
{
  "data": {
    "orderId": 456,
    "status": "CANCELLED",
    "cancelledAt": "2025-01-28T11:00:00Z",
    "cancelReason": "단순 변심"
  }
}
```

**Response 400 Bad Request (이미 결제됨)**
```json
{
  "code": "ORDER_ALREADY_PAID",
  "message": "이미 결제된 주문은 취소할 수 없습니다",
  "details": {
    "orderId": 456,
    "status": "PAID",
    "paidAt": "2025-01-28T10:05:00Z"
  }
}
```

**Response 400 Bad Request (이미 취소됨)**
```json
{
  "code": "ORDER_ALREADY_CANCELLED",
  "message": "이미 취소된 주문입니다"
}
```

---

## 결제 API

### 1. 결제 요청

**Endpoint**: `POST /payments`

**Request Body**
```json
{
  "orderId": 456,
  "paymentMethod": "CARD",
  "amount": 4500000,
  "cardInfo": {
    "cardNumber": "1234-5678-9012-3456",
    "expiryMonth": "12",
    "expiryYear": "2026",
    "cvv": "123",
    "cardholderName": "홍길동"
  }
}
```

**Validation Rules**
- `orderId`: 필수, 양의 정수
- `paymentMethod`: 필수, `CARD`, `VIRTUAL_ACCOUNT`, `BANK_TRANSFER` 중 하나
- `amount`: 필수, 주문의 최종 금액과 일치해야 함 (검증용)
- `cardInfo`: 결제 수단이 `CARD`일 경우 필수

**Note**: 쿠폰 할인은 주문 생성 시 이미 적용되어 `finalAmount`에 반영됨. 결제 시에는 주문의 최종 금액을 그대로 사용.

**Response 201 Created**
```json
{
  "data": {
    "paymentId": 789,
    "orderId": 456,
    "amount": 4500000,
    "paymentMethod": "CARD",
    "status": "SUCCESS",
    "transactionId": "txn_abc123",
    "paidAt": "2025-01-28T10:05:00Z"
  }
}
```

**Response 400 Bad Request (주문 상태 오류)**
```json
{
  "code": "INVALID_ORDER_STATUS",
  "message": "결제 대기 상태의 주문만 결제할 수 있습니다",
  "details": {
    "orderId": 456,
    "currentStatus": "PAID"
  }
}
```

**Response 400 Bad Request (금액 불일치)**
```json
{
  "code": "PAYMENT_AMOUNT_MISMATCH",
  "message": "결제 금액이 주문 금액과 일치하지 않습니다",
  "details": {
    "expectedAmount": 4500000,
    "requestedAmount": 5000000
  }
}
```

**Response 400 Bad Request (결제 실패)**
```json
{
  "code": "PAYMENT_FAILED",
  "message": "결제에 실패했습니다",
  "details": {
    "paymentId": 789,
    "failReason": "카드 한도 초과",
    "pgResponse": {
      "code": "CARD_LIMIT_EXCEEDED",
      "message": "Card limit exceeded"
    }
  }
}
```

**Response 408 Request Timeout**
```json
{
  "code": "PAYMENT_TIMEOUT",
  "message": "결제 처리 시간이 초과되었습니다"
}
```

---

### 2. 결제 상태 조회

**Endpoint**: `GET /payments/{paymentId}`

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `paymentId` | integer | 결제 ID |

**Response 200 OK**
```json
{
  "data": {
    "paymentId": 789,
    "orderId": 456,
    "amount": 4500000,
    "discountAmount": 500000,
    "finalAmount": 4500000,
    "paymentMethod": "CARD",
    "status": "SUCCESS",
    "transactionId": "txn_abc123",
    "paidAt": "2025-01-28T10:05:00Z",
    "createdAt": "2025-01-28T10:04:00Z"
  }
}
```

**Response 404 Not Found**
```json
{
  "code": "PAYMENT_NOT_FOUND",
  "message": "결제 정보를 찾을 수 없습니다"
}
```

---

### 3. 주문별 결제 조회

**Endpoint**: `GET /orders/{orderId}/payment`

**Response 200 OK**
```json
{
  "data": {
    "paymentId": 789,
    "orderId": 456,
    "amount": 4500000,
    "paymentMethod": "CARD",
    "status": "SUCCESS",
    "paidAt": "2025-01-28T10:05:00Z"
  }
}
```

**Response 404 Not Found (결제 내역 없음)**
```json
{
  "code": "PAYMENT_NOT_FOUND",
  "message": "해당 주문의 결제 내역이 없습니다"
}
```

---

## 쿠폰 API

### 1. 쿠폰 발급 (선착순)

**Endpoint**: `POST /users/me/coupons`

**Request Body**
```json
{
  "couponId": 10
}
```

**Validation Rules**
- `couponId`: 필수, 양의 정수

**Response 201 Created**
```json
{
  "data": {
    "userCouponId": 1,
    "couponId": 10,
    "userId": 123,
    "code": "WELCOME2024",
    "name": "신규 가입 환영 쿠폰",
    "discountType": "FIXED_AMOUNT",
    "discountValue": 10000,
    "minOrderAmount": 50000,
    "isUsed": false,
    "issuedAt": "2025-01-28T10:00:00Z",
    "expiresAt": "2025-12-31T23:59:59Z"
  }
}
```

**Response 409 Conflict (중복 발급)**
```json
{
  "code": "COUPON_ALREADY_ISSUED",
  "message": "이미 발급받은 쿠폰입니다",
  "details": {
    "couponId": 10,
    "issuedAt": "2025-01-20T10:00:00Z"
  }
}
```

**Response 409 Conflict (수량 소진)**
```json
{
  "code": "COUPON_EXHAUSTED",
  "message": "쿠폰이 모두 소진되었습니다"
}
```

**Response 400 Bad Request (만료된 쿠폰)**
```json
{
  "code": "COUPON_EXPIRED",
  "message": "만료된 쿠폰입니다",
  "details": {
    "expiresAt": "2025-01-27T23:59:59Z"
  }
}
```

---

### 2. 보유 쿠폰 조회

**Endpoint**: `GET /users/me/coupons`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|-----|------|
| `isUsed` | boolean | N | 사용 여부 필터 (true/false) |

**Response 200 OK**
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
      "minOrderAmount": 50000,
      "isUsed": false,
      "issuedAt": "2025-01-15T09:00:00Z",
      "expiresAt": "2025-12-31T23:59:59Z"
    }
  ]
}
```

---

### 3. 사용 가능한 쿠폰 조회

**Endpoint**: `GET /coupons/available`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|-----|------|
| `orderAmount` | integer | N | 주문 금액 (최소 금액 필터링용) |

**Response 200 OK**
```json
{
  "data": [
    {
      "couponId": 10,
      "code": "WELCOME2024",
      "name": "신규 가입 환영 쿠폰",
      "description": "모든 상품 10% 할인",
      "discountType": "PERCENTAGE",
      "discountValue": 10,
      "minOrderAmount": 50000,
      "maxDiscountAmount": 20000,
      "totalQuantity": 1000,
      "remainingQuantity": 342,
      "startsAt": "2025-01-01T00:00:00Z",
      "endsAt": "2025-12-31T23:59:59Z",
      "status": "ACTIVE"
    }
  ]
}
```

---

## 에러 응답

### 에러 응답 형식

```json
{
  "code": "ERROR_CODE",
  "message": "사용자 친화적인 에러 메시지",
  "details": {
    "field": "추가 정보"
  }
}
```

### 에러 코드 목록

| 에러 코드 | HTTP 상태 | 설명 |
|----------|-----------|------|
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없음 |
| `PRODUCT_OUT_OF_STOCK` | 400 | 품절된 상품 |
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니 항목을 찾을 수 없음 |
| `EMPTY_CART` | 400 | 빈 장바구니 |
| `ORDER_NOT_FOUND` | 404 | 주문을 찾을 수 없음 |
| `ORDER_ALREADY_PAID` | 400 | 이미 결제된 주문 |
| `ORDER_ALREADY_CANCELLED` | 400 | 이미 취소된 주문 |
| `ORDER_EXPIRED` | 400 | 만료된 주문 (15분 초과) |
| `INVALID_ORDER_STATUS` | 400 | 잘못된 주문 상태 |
| `PAYMENT_NOT_FOUND` | 404 | 결제 정보를 찾을 수 없음 |
| `PAYMENT_FAILED` | 400 | 결제 실패 |
| `PAYMENT_TIMEOUT` | 408 | 결제 타임아웃 |
| `PAYMENT_AMOUNT_MISMATCH` | 400 | 결제 금액 불일치 |
| `INSUFFICIENT_STOCK` | 409 | 재고 부족 |
| `COUPON_NOT_FOUND` | 404 | 쿠폰을 찾을 수 없음 |
| `COUPON_ALREADY_ISSUED` | 409 | 이미 발급받은 쿠폰 |
| `COUPON_EXHAUSTED` | 409 | 쿠폰 수량 소진 |
| `COUPON_ALREADY_USED` | 400 | 이미 사용된 쿠폰 |
| `COUPON_EXPIRED` | 400 | 만료된 쿠폰 |
| `COUPON_MIN_ORDER_AMOUNT_NOT_MET` | 400 | 최소 주문 금액 미달 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `VALIDATION_ERROR` | 400 | 입력 검증 실패 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

### Validation Error 예시

```json
{
  "code": "VALIDATION_ERROR",
  "message": "입력 값 검증에 실패했습니다",
  "details": {
    "fields": [
      {
        "field": "quantity",
        "rejectedValue": -1,
        "message": "수량은 1 이상이어야 합니다"
      },
      {
        "field": "deliveryAddress",
        "rejectedValue": "",
        "message": "배송 주소는 필수입니다"
      }
    ]
  }
}
```
---

## 참고 문서

- [Requirements Specification](./requirements.md)
- [User Stories](./user-stories.md)
- [Order Flow](./order-flow.md)
- [Data Models](./data-models.md)
