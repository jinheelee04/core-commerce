-- ====================================
-- E-commerce Service - DDL Script
-- Database: MySQL 8.0+
-- ====================================

-- ====================================
-- 1. User (사용자)
-- ====================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
    email VARCHAR(255) NOT NULL COMMENT '이메일',
    name VARCHAR(100) NOT NULL COMMENT '사용자 이름',
    phone VARCHAR(20) COMMENT '전화번호',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';

-- ====================================
-- 2. UserAddress (사용자 배송지)
-- ====================================
CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '배송지 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    address_name VARCHAR(50) COMMENT '배송지명 (예: 집, 회사)',
    recipient_name VARCHAR(100) NOT NULL COMMENT '수령인 이름',
    recipient_phone VARCHAR(20) NOT NULL COMMENT '수령인 전화번호',
    postal_code VARCHAR(10) NOT NULL COMMENT '우편번호',
    address VARCHAR(200) NOT NULL COMMENT '기본 주소',
    address_detail VARCHAR(200) COMMENT '상세 주소',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '기본 배송지 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_addresses_user_address_name (user_id, address_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 배송지';

-- ====================================
-- 3. Category (카테고리)
-- ====================================
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    parent_id BIGINT COMMENT '상위 카테고리 ID (NULL이면 최상위)',
    name VARCHAR(100) NOT NULL COMMENT '카테고리명',
    name_en VARCHAR(100) COMMENT '영문 카테고리명',
    level INT NOT NULL COMMENT '카테고리 레벨 (1=대분류, 2=중분류, 3=소분류)',
    display_order INT NOT NULL DEFAULT 0 COMMENT '표시 순서',
    image_url VARCHAR(500) COMMENT '카테고리 이미지 URL',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리';

-- ====================================
-- 4. Brand (브랜드)
-- ====================================
CREATE TABLE IF NOT EXISTS brands (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '브랜드 ID',
    name VARCHAR(100) NOT NULL COMMENT '브랜드명',
    name_en VARCHAR(100) COMMENT '영문 브랜드명',
    logo_url VARCHAR(500) COMMENT '브랜드 로고 URL',
    description TEXT COMMENT '브랜드 설명',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_brands_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='브랜드';

-- ====================================
-- 5. Product (상품)
-- ====================================
CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '상품 ID',
    category_id BIGINT NOT NULL COMMENT '카테고리 ID',
    brand_id BIGINT COMMENT '브랜드 ID',
    name VARCHAR(255) NOT NULL COMMENT '상품명',
    description TEXT COMMENT '상품 설명',
    price BIGINT NOT NULL COMMENT '가격 (원 단위)',
    image_url VARCHAR(500) COMMENT '이미지 URL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '상품 상태 (ACTIVE, INACTIVE, DISCONTINUED)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품';

-- ====================================
-- 6. Inventory (재고)
-- ====================================
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '재고 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    stock INT NOT NULL DEFAULT 0 COMMENT '현재 재고',
    reserved_stock INT NOT NULL DEFAULT 0 COMMENT '예약 재고',
    low_stock_threshold INT NOT NULL DEFAULT 10 COMMENT '낮은 재고 기준',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고';

-- ====================================
-- 7. Cart (장바구니)
-- ====================================
CREATE TABLE IF NOT EXISTS carts (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '장바구니 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_carts_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장바구니';

-- ====================================
-- 8. CartItem (장바구니 항목)
-- ====================================
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '장바구니 항목 ID',
    cart_id BIGINT NOT NULL COMMENT '장바구니 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '수량',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_cart_product (cart_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장바구니 항목';

-- ====================================
-- 9. Order (주문)
-- ====================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '주문 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    user_address_id BIGINT COMMENT '사용자 배송지 ID (참고용)',
    order_number VARCHAR(50) NOT NULL COMMENT '주문번호 (예: ORD-20250128-001)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '주문 상태 (PENDING, PAID, CONFIRMED, CANCELLED)',
    items_total BIGINT NOT NULL DEFAULT 0 COMMENT '상품 합계 금액',
    discount_amount BIGINT NOT NULL DEFAULT 0 COMMENT '할인 금액',
    final_amount BIGINT NOT NULL DEFAULT 0 COMMENT '최종 결제 금액',
    recipient_name VARCHAR(100) NOT NULL COMMENT '수령인 이름 (스냅샷)',
    recipient_phone VARCHAR(20) NOT NULL COMMENT '수령인 전화번호 (스냅샷)',
    postal_code VARCHAR(10) NOT NULL COMMENT '우편번호 (스냅샷)',
    address VARCHAR(200) NOT NULL COMMENT '기본 주소 (스냅샷)',
    address_detail VARCHAR(200) COMMENT '상세 주소 (스냅샷)',
    delivery_memo TEXT COMMENT '배송 메모',
    expires_at TIMESTAMP COMMENT '만료 시간 (15분)',
    paid_at TIMESTAMP COMMENT '결제 완료 시간',
    cancelled_at TIMESTAMP COMMENT '취소 시간',
    cancel_reason VARCHAR(255) COMMENT '취소 사유',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_number (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문';

-- ====================================
-- 10. OrderItem (주문 항목)
-- ====================================
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '주문 항목 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    quantity INT NOT NULL COMMENT '수량',
    unit_price BIGINT NOT NULL COMMENT '단가 (주문 시점 가격)',
    subtotal BIGINT NOT NULL COMMENT '소계 (unit_price * quantity)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 항목';

-- ====================================
-- 11. OrderStatusHistory (주문 상태 이력)
-- ====================================
CREATE TABLE IF NOT EXISTS order_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이력 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    previous_status VARCHAR(20) COMMENT '이전 상태',
    new_status VARCHAR(20) NOT NULL COMMENT '새 상태',
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '상태 변경 일시',
    changed_by BIGINT COMMENT '변경자 ID (관리자 or 시스템)',
    reason TEXT COMMENT '변경 사유',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 상태 이력';

-- ====================================
-- 12. Payment (결제)
-- ====================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    amount BIGINT NOT NULL COMMENT '결제 금액',
    payment_method VARCHAR(20) NOT NULL COMMENT '결제 수단 (CARD, VIRTUAL_ACCOUNT, PHONE)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '결제 상태 (PENDING, SUCCESS, FAILED)',
    transaction_id VARCHAR(100) COMMENT 'PG 거래 ID',
    fail_reason TEXT COMMENT '실패 사유',
    paid_at TIMESTAMP COMMENT '결제 완료 시간',
    failed_at TIMESTAMP COMMENT '결제 실패 시간',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='결제';

-- ====================================
-- 13. Coupon (쿠폰)
-- ====================================
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '쿠폰 ID',
    code VARCHAR(50) NOT NULL COMMENT '쿠폰 코드',
    name VARCHAR(255) NOT NULL COMMENT '쿠폰명',
    description TEXT COMMENT '설명',
    discount_type VARCHAR(20) NOT NULL COMMENT '할인 타입 (PERCENTAGE, FIXED_AMOUNT)',
    discount_value INT NOT NULL COMMENT '할인 값 (%, 원)',
    min_order_amount BIGINT NOT NULL DEFAULT 0 COMMENT '최소 주문 금액',
    max_discount_amount BIGINT COMMENT '최대 할인 금액',
    total_quantity INT NOT NULL COMMENT '총 발급 수량',
    remaining_quantity INT NOT NULL COMMENT '잔여 수량',
    starts_at TIMESTAMP NOT NULL COMMENT '시작일시',
    ends_at TIMESTAMP NOT NULL COMMENT '종료일시',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '쿠폰 상태 (ACTIVE, INACTIVE, EXPIRED)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupons_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰';

-- ====================================
-- 14. UserCoupon (사용자 쿠폰)
-- ====================================
CREATE TABLE IF NOT EXISTS user_coupons (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 쿠폰 ID',
    coupon_id BIGINT NOT NULL COMMENT '쿠폰 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    order_id BIGINT COMMENT '주문 ID (사용 시)',
    is_used BOOLEAN NOT NULL DEFAULT FALSE COMMENT '사용 여부',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급일시',
    used_at TIMESTAMP COMMENT '사용일시',
    expires_at TIMESTAMP NOT NULL COMMENT '만료일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupons_coupon_user (coupon_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 쿠폰';

-- ====================================
-- 인덱스는 STEP08에서 추가 예정
-- FK 제약조건은 애플리케이션 레벨에서 관리
-- ====================================
