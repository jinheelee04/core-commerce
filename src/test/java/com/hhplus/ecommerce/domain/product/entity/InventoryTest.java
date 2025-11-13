package com.hhplus.ecommerce.domain.product.entity;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.category.entity.Category;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Inventory 도메인 엔티티 단위 테스트")
class InventoryTest {

    private Product mockProduct;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        mockProduct = mock(Product.class);
        inventory = new Inventory(mockProduct, 100, 10);
    }

    @Test
    @DisplayName("가용 재고 계산 - stock - reservedStock")
    void getAvailableStock_Success() {
        // given
        Inventory testInventory = new Inventory(mockProduct, 100, 10);
        // Simulate reserved stock using reflection
        try {
            java.lang.reflect.Field field = Inventory.class.getDeclaredField("reservedStock");
            field.setAccessible(true);
            field.set(testInventory, 30);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // when
        int availableStock = testInventory.getAvailableStock();

        // then
        assertThat(availableStock).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 예약 성공")
    void reserve_Success() {
        // given
        int quantityToReserve = 10;

        // when
        inventory.reserve(quantityToReserve);

        // then
        assertThat(inventory.getReservedStock()).isEqualTo(10);
        assertThat(inventory.getAvailableStock()).isEqualTo(90);
        assertThat(inventory.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 예약 실패 - 가용 재고 부족")
    void reserve_InsufficientStock_ThrowsException() {
        // given
        int quantityToReserve = 101;

        // when & then
        assertThatThrownBy(() -> inventory.reserve(quantityToReserve))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);

        assertThat(inventory.getReservedStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 예약 실패 - 일부는 이미 예약된 상태에서 추가 예약 시도")
    void reserve_PartiallyReserved_ThrowsException() {
        // given
        inventory.reserve(50);

        // when & then
        assertThatThrownBy(() -> inventory.reserve(60))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);

        assertThat(inventory.getReservedStock()).isEqualTo(50);
        assertThat(inventory.getAvailableStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("예약 해제 성공")
    void releaseReservation_Success() {
        // given
        inventory.reserve(30);

        // when
        inventory.releaseReservation(10);

        // then
        assertThat(inventory.getReservedStock()).isEqualTo(20);
        assertThat(inventory.getAvailableStock()).isEqualTo(80);
        assertThat(inventory.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("예약 해제 실패 - 예약된 재고보다 많이 해제 시도")
    void releaseReservation_ExceedsReserved_ThrowsException() {
        // given
        inventory.reserve(20);

        // when & then
        assertThatThrownBy(() -> inventory.releaseReservation(30))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_RESERVED_STOCK);

        assertThat(inventory.getReservedStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("예약 해제 실패 - 예약된 재고가 없는데 해제 시도")
    void releaseReservation_NoReservation_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> inventory.releaseReservation(10))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_RESERVED_STOCK);
    }

    @Test
    @DisplayName("예약 확정 성공 - 실제 재고 차감")
    void confirmReservation_Success() {
        // given
        inventory.reserve(20);

        // when
        inventory.confirmReservation(20);

        // then
        assertThat(inventory.getStock()).isEqualTo(80);
        assertThat(inventory.getReservedStock()).isEqualTo(0);
        assertThat(inventory.getAvailableStock()).isEqualTo(80);
    }

    @Test
    @DisplayName("예약 확정 실패 - 예약된 재고보다 많이 확정 시도")
    void confirmReservation_ExceedsReserved_ThrowsException() {
        // given
        inventory.reserve(10);

        // when & then
        assertThatThrownBy(() -> inventory.confirmReservation(20))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_RESERVED_STOCK);

        assertThat(inventory.getStock()).isEqualTo(100);
        assertThat(inventory.getReservedStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("예약 확정 실패 - 실제 재고보다 많이 확정 시도")
    void confirmReservation_ExceedsStock_ThrowsException() {
        // given
        Inventory smallInventory = new Inventory(mockProduct, 10, 5);
        // Simulate reserved stock
        try {
            java.lang.reflect.Field field = Inventory.class.getDeclaredField("reservedStock");
            field.setAccessible(true);
            field.set(smallInventory, 5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // when & then
        assertThatThrownBy(() -> smallInventory.confirmReservation(15))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_RESERVED_STOCK);
    }

    @Test
    @DisplayName("재고 추가 성공")
    void addStock_Success() {
        // given
        int quantityToAdd = 50;

        // when
        inventory.addStock(quantityToAdd);

        // then
        assertThat(inventory.getStock()).isEqualTo(150);
        assertThat(inventory.getAvailableStock()).isEqualTo(150);
    }

    @Test
    @DisplayName("재고 추가 후 가용 재고 계산 정확성")
    void addStock_WithReservation_CalculatesCorrectly() {
        // given
        inventory.reserve(30);

        // when
        inventory.addStock(50);

        // then
        assertThat(inventory.getStock()).isEqualTo(150);
        assertThat(inventory.getReservedStock()).isEqualTo(30);
        assertThat(inventory.getAvailableStock()).isEqualTo(120);
    }

    @Test
    @DisplayName("재고 부족 여부 확인 - 정상 재고")
    void isLowStock_NormalStock_ReturnsFalse() {
        // given
        Inventory normalInventory = new Inventory(mockProduct, 100, 10);

        // when
        boolean isLow = normalInventory.isLowStock();

        // then
        assertThat(isLow).isFalse();
    }

    @Test
    @DisplayName("재고 부족 여부 확인 - 재고 부족")
    void isLowStock_LowStock_ReturnsTrue() {
        // given
        Inventory lowInventory = new Inventory(mockProduct, 15, 10);
        // Simulate reserved stock
        try {
            java.lang.reflect.Field field = Inventory.class.getDeclaredField("reservedStock");
            field.setAccessible(true);
            field.set(lowInventory, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // when
        boolean isLow = lowInventory.isLowStock();

        // then
        assertThat(isLow).isTrue();
        assertThat(lowInventory.getAvailableStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고 부족 여부 확인 - 임계값과 동일")
    void isLowStock_ExactlyThreshold_ReturnsTrue() {
        // given
        Inventory exactInventory = new Inventory(mockProduct, 10, 10);

        // when
        boolean isLow = exactInventory.isLowStock();

        // then
        assertThat(isLow).isTrue();
    }

    @Test
    @DisplayName("전체 플로우 - 예약 → 일부 해제 → 확정")
    void fullFlow_ReserveReleaseConfirm() {
        // given
        Inventory flowInventory = new Inventory(mockProduct, 100, 10);

        // when - 50개 예약
        flowInventory.reserve(50);
        assertThat(flowInventory.getReservedStock()).isEqualTo(50);
        assertThat(flowInventory.getAvailableStock()).isEqualTo(50);

        // when - 10개 예약 해제
        flowInventory.releaseReservation(10);
        assertThat(flowInventory.getReservedStock()).isEqualTo(40);
        assertThat(flowInventory.getAvailableStock()).isEqualTo(60);

        // when - 40개 확정
        flowInventory.confirmReservation(40);

        // then
        assertThat(flowInventory.getStock()).isEqualTo(60);
        assertThat(flowInventory.getReservedStock()).isEqualTo(0);
        assertThat(flowInventory.getAvailableStock()).isEqualTo(60);
    }

    @Test
    @DisplayName("동시 예약 시나리오 - 가용 재고 한계 테스트")
    void concurrentReservation_ExhaustsStock() {
        // given
        Inventory concurrentInventory = new Inventory(mockProduct, 100, 10);

        // when - 3번에 걸쳐 예약
        concurrentInventory.reserve(40);
        concurrentInventory.reserve(30);
        concurrentInventory.reserve(30);

        // then
        assertThat(concurrentInventory.getReservedStock()).isEqualTo(100);
        assertThat(concurrentInventory.getAvailableStock()).isEqualTo(0);

        // when & then - 1개 더 예약 시도
        assertThatThrownBy(() -> concurrentInventory.reserve(1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);
    }
}
