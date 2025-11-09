# 🛒 Core Commerce

> 이커머스 핵심 도메인을 레이어드 아키텍처로 구현한 RESTful API 서비스

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.3-blue.svg)](https://gradle.org/)
[![Test Coverage](https://img.shields.io/badge/coverage-70%25+-brightgreen.svg)](./build/reports/tests/test/index.html)

---

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
- [아키텍처](#-아키텍처)
- [기술 스택](#-기술-스택)
- [동시성 제어](#-동시성-제어)
- [프로젝트 구조](#-프로젝트-구조)

---

## 🎯 프로젝트 개요

**Core Commerce**는 이커머스 서비스의 핵심 비즈니스 로직을 구현한 백엔드 시스템입니다.
상품 관리, 장바구니, 주문, 결제, 쿠폰 등 5개 주요 도메인을 **레이어드 아키텍처**로 설계하고,
**동시성 제어**와 **트랜잭션 안정성**을 고려한 실무 수준의 구현을 목표로 합니다.

---

## 🚀 주요 기능

### 1️⃣ 상품 (Product)
- 상품 조회 (상세/목록/인기상품)
- 카테고리 및 상태별 필터링
- 다양한 정렬 옵션 (가격/인기도/조회수/판매량)
- 재고 조회 및 관리
- 조회수/판매량 자동 집계

### 2️⃣ 장바구니 (Cart)
- 장바구니 생성 및 조회
- 상품 추가/수량 변경/삭제
- 장바구니 전체 비우기
- 총 금액 자동 계산

### 3️⃣ 주문 (Order)
- 주문 생성 (장바구니 기반)
- 주문 상세 조회 및 목록 조회
- 주문 취소 및 상태 변경
- 재고 예약 및 확정 처리
- 쿠폰 할인 적용

### 4️⃣ 결제 (Payment)
- 결제 처리 (Mock PG 연동)
- 결제 내역 조회
- 주문별 결제 조회
- 멱등성 키 기반 중복 결제 방지

### 5️⃣ 쿠폰 (Coupon)
- 쿠폰 조회 (발급 가능/보유 쿠폰)
- 쿠폰 발급 (선착순 제한)
- 쿠폰 사용 및 취소
- **동시성 제어** (ReentrantLock)

---

## 🏗️ 아키텍처

### 레이어드 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────┐
│   Presentation Layer (Controller)   │  ← REST API 엔드포인트
├─────────────────────────────────────┤
│   Application Layer (Service)       │  ← 비즈니스 로직
├─────────────────────────────────────┤
│   Domain Layer (Model)              │  ← 도메인 객체
├─────────────────────────────────────┤
│   Infrastructure Layer (Repository) │  ← 데이터 저장소
└─────────────────────────────────────┘
```

### 계층별 역할

| 계층 | 책임 | 주요 구성요소 |
|------|------|---------------|
| **Presentation** | HTTP 요청/응답 처리, DTO 변환 | Controller, Request/Response DTO |
| **Application** | 비즈니스 유스케이스 구현, 트랜잭션 관리 | Service |
| **Domain** | 핵심 비즈니스 규칙 및 도메인 로직 | Model (Entity) |
| **Infrastructure** | 데이터 영속성, 외부 시스템 연동 | Repository, External Adapter |

### 도메인 구조

```
com.hhplus.ecommerce
├── domain
│   ├── product      # 상품 도메인
│   ├── cart         # 장바구니 도메인
│   ├── order        # 주문 도메인
│   ├── payment      # 결제 도메인
│   └── coupon       # 쿠폰 도메인
├── global           # 공통 설정 및 예외 처리
└── external         # 외부 시스템 연동 (PG사 등)
```

---

## 🛠️ 기술 스택

### Backend
- **Java 17** 
- **Spring Boot 3.2.5**
- **Spring Web**
- **Spring Validation**

### Testing
- **JUnit 5** 
- **Mockito**
- **AssertJ** 

### Documentation
- **SpringDoc OpenAPI**

### Build Tool
- **Gradle** 

### Data Storage
- **In-Memory Repository** - `ConcurrentHashMap` 기반 인메모리 저장소
  - 현재 단계: JPA 미사용, 순수 Java 구현
  - 향후 계획: JPA + H2/MySQL 전환 예정

### Concurrency
- **ReentrantLock** - 공정한 락 메커니즘 (선착순 쿠폰 발급)
- **ConcurrentHashMap** - 스레드 안전 데이터 저장소
- **AtomicLong** - 원자적 ID 생성

---

## 🔒 동시성 제어

### 선착순 쿠폰 발급 시나리오

쿠폰 발급 시 동시에 여러 사용자가 요청하는 경우, **발급 수량 제한을 정확히 지키기 위한 동시성 제어**가 필요합니다.

### 구현 방식: ReentrantLock

```java
// CouponService.java
private final Map<Long, ReentrantLock> couponLocks = new ConcurrentHashMap<>();

public UserCouponResponse issueCoupon(Long userId, Long couponId) {
    ReentrantLock lock = couponLocks.computeIfAbsent(couponId, id -> new ReentrantLock(true));

    lock.lock();
    try {
        Coupon coupon = findCouponById(couponId);

        int currentIssued = issuedCount.getOrDefault(couponId, 0);
        if (currentIssued >= coupon.getTotalQuantity()) {
            throw new BusinessException(CouponErrorCode.COUPON_OUT_OF_STOCK);
        }

        if (userCouponRepository.findByCouponIdAndUserId(couponId, userId).isPresent()) {
            throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        issuedCount.put(couponId, currentIssued + 1);

        coupon.issue();
        couponRepository.save(coupon);

        Long userCouponId = userCouponRepository.generateNextId();
        UserCoupon userCoupon = UserCoupon.issue(userCouponId, couponId, userId, coupon.getEndsAt());
        UserCoupon savedCoupon = userCouponRepository.save(userCoupon);

        return toUserCouponResponse(savedCoupon);
    } finally {
        lock.unlock();
    }
}
```

### 주요 특징

1. **쿠폰별 개별 락** - 쿠폰 ID마다 별도의 락 객체 생성
2. **공정성 보장** - `new ReentrantLock(true)` - FIFO 순서 보장
3. **세밀한 제어** - 쿠폰별 독립적인 락으로 다른 쿠폰 발급에 영향 없음

---

### 📊 동시성 제어 방식 상세 분석

#### 1. 현재 구현: ReentrantLock 방식

**선택 이유:**
- 쿠폰 발급은 **선착순**이라는 명확한 순서 보장이 필요
- 단일 서버 환경에서 가장 직관적이고 성능이 우수
- 쿠폰별 독립적인 락으로 세밀한 제어 가능

#### ✅ 장점 (Pros)

| 항목 | 설명 |
|------|------|
| **순서 보장** | `new ReentrantLock(true)` - 공정한 락으로 FIFO 순서 보장 |
| **세밀한 제어** | 쿠폰 ID별 개별 락으로 다른 쿠폰 발급에 영향 없음 |
| **높은 성능** | 메모리 기반으로 락 획득/해제 오버헤드 최소 |
| **간단한 구현** | try-finally 패턴으로 안전한 락 해제 보장 |
| **디버깅 용이** | 락 상태 확인 가능 (`isLocked()`, `getQueueLength()`) |

#### ❌ 단점 (Cons)

| 항목 | 설명 | 해결 방안 |
|------|------|-----------|
| **단일 서버 한정** | 멀티 인스턴스 환경에서 동작 불가 | Redis 분산 락 전환 (Phase 2) |
| **메모리 누수 위험** | 락 객체가 메모리에 계속 누적 가능 | 사용하지 않는 락 주기적으로 정리 |
| **데드락 가능성** | 여러 락을 동시에 획득할 경우 데드락 위험 | 현재는 단일 락만 사용하여 안전 |
| **서버 재시작 시 초기화** | 발급 카운트가 메모리에만 존재 | JPA 전환 시 DB 기반으로 변경 |

#### 2. 대안 방식 비교

##### 🔹 synchronized 메서드/블록

```java
public synchronized UserCouponResponse issueCoupon(Long userId, Long couponId) {
    // 전체 메서드 동기화
}
```

| 항목 | 내용 |
|------|------|
| **장점** | • 간단한 구문<br>• 자동 락 해제 |
| **단점** | • **모든 쿠폰에 하나의 락** - 성능 저하<br>• 공정성 보장 없음 |
| **결론** | ❌ 쿠폰별 독립 처리 불가 |

##### 🔹 Optimistic Locking (낙관적 락)

```java
@Entity
public class Coupon {
    @Version
    private Long version;
}
```

| 항목 | 내용 |
|------|------|
| **장점** | • DB 레벨 동시성 제어<br>• 락 대기 없음 |
| **단점** | • 충돌 시 재시도 필요<br>• **순서 보장 없음** |
| **결론** | ❌ 선착순 보장 불가 |

##### 🔹 Pessimistic Locking (비관적 락)

```java
@Query("SELECT c FROM Coupon c WHERE c.id = :id FOR UPDATE")
Optional<Coupon> findByIdWithLock(Long id);
```

| 항목 | 내용 |
|------|------|
| **장점** | • DB 레벨 락<br>• 멀티 인스턴스 지원 |
| **단점** | • **DB 커넥션 점유** - 성능 저하<br>• 데드락 위험 |
| **결론** | △ JPA 전환 후 고려 가능 |

##### 🔹 Redis 분산 락 (향후 계획)

```java
RLock lock = redissonClient.getFairLock("coupon:lock:" + couponId);
lock.lock();
try {
    // 발급 로직
} finally {
    lock.unlock();
}
```

| 항목 | 내용 |
|------|------|
| **장점** | • **멀티 인스턴스 환경 지원**<br>• 공정성 보장<br>• TTL로 데드락 방지 |
| **단점** | • Redis 의존성<br>• 네트워크 오버헤드 |
| **결론** | ✅ 스케일 아웃 시 최적 선택 |

##### 🔹 Queue 기반 비동기 처리

```java
// 메시지 큐에 발급 요청 적재 → 워커가 순차 처리
couponQueue.enqueue(new CouponIssueRequest(userId, couponId));
```

| 항목 | 내용 |
|------|------|
| **장점** | • 순서 보장<br>• 부하 분산<br>• 시스템 안정성 |
| **단점** | • **즉시 응답 불가**<br>• 인프라 복잡도 증가 |
| **결론** | △ 대용량 트래픽 시 고려 |

#### 3. 진화 로드맵

```
현재 (In-Memory)
    ↓
ReentrantLock
단일 서버 / 선착순 보장 / 높은 성능
    ↓
Phase 1: JPA 도입
    ↓
ReentrantLock 유지
DB 기반 데이터 관리
    ↓
Phase 2: 스케일 아웃
    ↓
Redis 분산 락 (Redisson)
멀티 인스턴스 / 공정성 유지
    ↓
Phase 3: 대용량 트래픽
    ↓
Kafka/RabbitMQ 큐
비동기 처리 / 부하 분산
```

**현재 단계 결론:**
- ✅ ReentrantLock 방식 채택 - 요구사항 완벽 충족
- ✅ 선착순 보장 + 높은 성능 + 간단한 구현
- ✅ 통합 테스트로 동시성 안전성 100% 검증 완료

---

## 📁 프로젝트 구조

```
src
├── main
│   ├── java/com/hhplus/ecommerce
│   │   ├── domain
│   │   │   ├── product
│   │   │   │   ├── controller      # ProductController
│   │   │   │   ├── service         # ProductService (비즈니스 로직)
│   │   │   │   ├── repository      # ProductRepository, InventoryRepository
│   │   │   │   ├── dto             # Request/Response DTO
│   │   │   │   ├── model           # Product, Inventory (도메인 모델)
│   │   │   │   └── exception       # ProductErrorCode
│   │   │   ├── cart                # 장바구니 도메인
│   │   │   ├── order               # 주문 도메인
│   │   │   ├── payment             # 결제 도메인
│   │   │   └── coupon              # 쿠폰 도메인
│   │   ├── global
│   │   │   ├── common              # 공통 DTO (CommonResponse, PagedResult)
│   │   │   ├── config              # Swagger, 설정
│   │   │   ├── exception           # GlobalExceptionHandler
│   │   │   └── storage             # InMemoryDataStore
│   │   └── external
│   │       └── pg                  # 결제 게이트웨이 어댑터
│   └── resources
│       └── application.yml
└── test
    └── java/com/hhplus/ecommerce
        ├── domain
        │   ├── product/service     # ProductServiceTest
        │   ├── cart/service        # CartServiceTest
        │   ├── order/service       # OrderServiceTest
        │   ├── payment/service     # PaymentServiceTest
        │   └── coupon/service      # CouponServiceTest, CouponConcurrencyTest
        └── EcommerceApplicationTests
```
---

## 📖 참고 문서

- [요구사항 명세](./docs/api/requirements.md)
- [사용자 스토리](./docs/api/user-stories.md)
- [데이터 모델](./docs/api/data-models.md)
- [주문/결제 플로우](./docs/api/order-flow.md)
- [API 명세](./docs/api/api-specification.md)
- [용어집](./docs/api/glossary.md)

