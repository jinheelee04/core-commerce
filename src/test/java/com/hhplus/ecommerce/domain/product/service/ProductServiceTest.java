package com.hhplus.ecommerce.domain.product.service;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.category.entity.Category;
import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.entity.ProductStatus;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.domain.product.repository.InventoryRepository;
import com.hhplus.ecommerce.domain.product.repository.ProductRepository;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("ProductService 단위 테스트")
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    private static final Long PRODUCT_ID = 1L;
    private static final Long CATEGORY_ID = 100L;

    private Product mockProduct;
    private Inventory mockInventory;
    private Category mockCategory;
    private Brand mockBrand;

    @BeforeEach
    void setUp() {
        mockCategory = mock(Category.class);
        when(mockCategory.getId()).thenReturn(CATEGORY_ID);

        mockBrand = mock(Brand.class);
        when(mockBrand.getId()).thenReturn(10L);

        mockProduct = new Product(mockCategory, mockBrand, "테스트 상품", "상품 설명", 10000L, null);
        setId(mockProduct, PRODUCT_ID);

        mockInventory = new Inventory(mockProduct, 100, 10);
        setId(mockInventory, 200L);
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // === 상품 조회 테스트 ===

    @Test
    @DisplayName("상품 ID로 조회 성공")
    void findProductById_Success() {
        // Given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(mockProduct));

        // When
        Product result = productService.findProductById(PRODUCT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getName()).isEqualTo("테스트 상품");
    }

    @Test
    @DisplayName("상품 ID로 조회 실패 - 존재하지 않는 상품")
    void findProductById_NotFound() {
        // Given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.findProductById(PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_Success() {
        // Given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(mockProduct));
        given(inventoryRepository.findByProductId(PRODUCT_ID)).willReturn(Optional.of(mockInventory));

        // When
        ProductResponse result = productService.getProductDetail(PRODUCT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.name()).isEqualTo("테스트 상품");
        assertThat(result.stock()).isEqualTo(100);
    }

    @Test
    @DisplayName("상품 목록 조회 성공 - 카테고리 필터")
    void getProducts_WithCategoryFilter_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(mockProduct), pageable, 1);

        given(productRepository.findByDynamicFilters(CATEGORY_ID, ProductStatus.ACTIVE, pageable))
                .willReturn(productPage);
        given(inventoryRepository.findAllByProductIdIn(anyList()))
                .willReturn(List.of(mockInventory));

        // When
        var result = productService.getProducts(CATEGORY_ID, ProductStatus.ACTIVE, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("인기 상품 목록 조회 성공")
    void getPopularProducts_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        Page<Product> productPage = new PageImpl<>(List.of(mockProduct), pageable, 1);

        given(productRepository.findPopularProducts("sales", pageable)).willReturn(productPage);
        given(inventoryRepository.findAllByProductIdIn(anyList())).willReturn(List.of(mockInventory));

        // When
        var result = productService.getPopularProducts("sales", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
    }

    // === 재고 조회 테스트 ===

    @Test
    @DisplayName("재고 조회 성공")
    void getInventory_Success() {
        // Given
        given(inventoryRepository.findByProductId(PRODUCT_ID)).willReturn(Optional.of(mockInventory));

        // When
        Inventory result = productService.getInventory(PRODUCT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 조회 실패 - 재고 정보 없음")
    void getInventory_NotFound() {
        // Given
        given(inventoryRepository.findByProductId(PRODUCT_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getInventory(PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 부족 상품 조회 성공")
    void getLowStockProducts_Success() {
        // Given
        given(inventoryRepository.findLowStockInventories()).willReturn(List.of(mockInventory));

        // When
        List<Inventory> result = productService.getLowStockProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("여러 상품 재고 Map 조회 성공")
    void getInventoriesAsMap_Success() {
        // Given
        given(inventoryRepository.findAllByProductIdIn(anyList())).willReturn(List.of(mockInventory));

        // When
        Map<Long, Inventory> result = productService.getInventoriesAsMap(List.of(PRODUCT_ID));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(PRODUCT_ID)).isNotNull();
    }

    @Test
    @DisplayName("여러 상품 Map 조회 성공")
    void getProductsAsMap_Success() {
        // Given
        given(productRepository.findAllById(anyList())).willReturn(List.of(mockProduct));

        // When
        Map<Long, Product> result = productService.getProductsAsMap(List.of(PRODUCT_ID));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(PRODUCT_ID)).isNotNull();
        assertThat(result.get(PRODUCT_ID).getName()).isEqualTo("테스트 상품");
    }

    // === 재고 예약/확정/해제 테스트 ===

    @Test
    @DisplayName("재고 예약 성공")
    void reserveStock_Success() {
        // Given
        given(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).willReturn(Optional.of(mockInventory));

        // When
        productService.reserveStock(PRODUCT_ID, 10);

        // Then
        verify(inventoryRepository, times(1)).findByProductIdWithLock(PRODUCT_ID);
    }

    @Test
    @DisplayName("재고 예약 실패 - 재고 부족")
    void reserveStock_InsufficientStock() {
        // Given
        given(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).willReturn(Optional.of(mockInventory));

        // When & Then
        assertThatThrownBy(() -> productService.reserveStock(PRODUCT_ID, 200))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("재고 확정 성공")
    void confirmStockReservation_Success() {
        // Given
        mockInventory.reserve(10);
        given(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).willReturn(Optional.of(mockInventory));

        // When
        productService.confirmStockReservation(PRODUCT_ID, 10);

        // Then
        verify(inventoryRepository, times(1)).findByProductIdWithLock(PRODUCT_ID);
    }

    @Test
    @DisplayName("재고 예약 해제 성공")
    void releaseStockReservation_Success() {
        // Given
        mockInventory.reserve(10);
        given(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).willReturn(Optional.of(mockInventory));

        // When
        productService.releaseStockReservation(PRODUCT_ID, 10);

        // Then
        verify(inventoryRepository, times(1)).findByProductIdWithLock(PRODUCT_ID);
    }

    @Test
    @DisplayName("판매 수량 증가 성공")
    void incrementSalesCount_Success() {
        // Given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(mockProduct));

        // When
        productService.incrementSalesCount(PRODUCT_ID, 5);

        // Then
        verify(productRepository, times(1)).findById(PRODUCT_ID);
    }
}
