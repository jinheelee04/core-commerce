package com.hhplus.ecommerce.domain.product.service;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.domain.product.model.Inventory;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;
import com.hhplus.ecommerce.domain.product.repository.InventoryRepository;
import com.hhplus.ecommerce.domain.product.repository.ProductRepository;
import com.hhplus.ecommerce.global.common.dto.PageMeta;
import com.hhplus.ecommerce.global.common.dto.PagedResult;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testProduct = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .brand("테스트 브랜드")
                .imageUrl("https://example.com/image.jpg")
                .status(ProductStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        testInventory = Inventory.builder()
                .id(1L)
                .productId(1L)
                .stock(100)
                .reservedStock(0)
                .lowStockThreshold(10)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProduct_Success() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // when
        Product result = productService.findProductById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트 상품");
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("상품 조회 실패 - 존재하지 않는 상품")
    void getProduct_NotFound_ThrowsException() {
        // given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.findProductById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProductDetail_Success() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));

        // when
        ProductResponse response = productService.getProductDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("테스트 상품");
        assertThat(response.stock()).isEqualTo(100);
        verify(productRepository).findById(1L);
        verify(inventoryRepository).findByProductId(1L);
    }

    @Test
    @DisplayName("상품 목록 조회 성공 (카테고리/상태 필터링)")
    void getProducts_Filtered_Success() {
        // given
        when(productRepository.findAll()).thenReturn(List.of(testProduct));
        when(inventoryRepository.findAll()).thenReturn(List.of(testInventory));

        // when
        PagedResult<ProductResponse> result = productService.getProducts(
                ProductCategory.ELECTRONICS, ProductStatus.AVAILABLE, "price_asc", 0, 10
        );

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("테스트 상품");
        assertThat(result.meta()).isInstanceOf(PageMeta.class);
        verify(productRepository).findAll();
        verify(inventoryRepository).findAll();
    }

    @Test
    @DisplayName("재고 조회 성공")
    void getInventory_Success() {
        // given
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));

        // when
        Inventory result = productService.getInventory(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStock()).isEqualTo(100);
        verify(inventoryRepository).findByProductId(1L);
    }

    @Test
    @DisplayName("재고 조회 실패 - 존재하지 않는 상품")
    void getInventory_NotFound_ThrowsException() {
        // given
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getInventory(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 부족 상품 목록 조회 성공")
    void getLowStockProducts_Success() {
        // given
        Inventory lowStockInventory = Inventory.builder()
                .id(2L)
                .productId(2L)
                .stock(5)
                .reservedStock(0)
                .lowStockThreshold(10)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(inventoryRepository.findLowStockProducts()).thenReturn(List.of(lowStockInventory));

        // when
        List<Inventory> result = productService.getLowStockProducts();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLowStock()).isTrue();
        verify(inventoryRepository).findLowStockProducts();
    }

    @Test
    @DisplayName("상품 상세 조회 시 조회수 증가")
    void getProductDetail_IncrementsViewCount() {
        // given
        Product productWithoutViews = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .brand("테스트 브랜드")
                .imageUrl("https://example.com/image.jpg")
                .status(ProductStatus.AVAILABLE)
                .viewCount(5)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithoutViews));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));

        // when
        productService.getProductDetail(1L);

        // then
        assertThat(productWithoutViews.getViewCount()).isEqualTo(6);
        verify(productRepository).save(productWithoutViews);
    }

    @Test
    @DisplayName("판매량 증가 성공")
    void incrementSalesCount_Success() {
        // given
        Product productWithSales = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .description("테스트 설명")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .brand("테스트 브랜드")
                .imageUrl("https://example.com/image.jpg")
                .status(ProductStatus.AVAILABLE)
                .salesCount(10)  // 초기 판매량 10
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithSales));

        // when
        productService.incrementSalesCount(1L, 3);

        // then
        assertThat(productWithSales.getSalesCount()).isEqualTo(13);
        verify(productRepository).save(productWithSales);
    }

    @Test
    @DisplayName("인기 상품 조회 - 종합 점수 기준 정렬")
    void getPopularProducts_SortByPopularityScore() {
        // given
        Product product1 = Product.builder()
                .id(1L)
                .name("높은 판매량 상품")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(100)
                .salesCount(50)
                .createdAt(LocalDateTime.now())
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .name("높은 조회수 상품")
                .price(20000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(500)
                .salesCount(10)
                .createdAt(LocalDateTime.now())
                .build();

        Product product3 = Product.builder()
                .id(3L)
                .name("낮은 인기 상품")
                .price(15000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(50)
                .salesCount(5)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product1, product2, product3));
        when(inventoryRepository.findAll()).thenReturn(List.of(
                testInventory,
                Inventory.builder().id(2L).productId(2L).stock(100).reservedStock(0).lowStockThreshold(10).createdAt(LocalDateTime.now()).build(),
                Inventory.builder().id(3L).productId(3L).stock(100).reservedStock(0).lowStockThreshold(10).createdAt(LocalDateTime.now()).build()
        ));

        // when
        PagedResult<ProductResponse> result = productService.getPopularProducts(0, 10, "popular");

        // then
        assertThat(result.content()).hasSize(3);
        assertThat(result.content().get(2).name()).isEqualTo("낮은 인기 상품");
    }

    @Test
    @DisplayName("인기 상품 조회 - 조회수 기준 정렬")
    void getPopularProducts_SortByViews() {
        // given
        Product product1 = Product.builder()
                .id(1L)
                .name("조회수 많음")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(1000)
                .salesCount(10)
                .createdAt(LocalDateTime.now())
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .name("조회수 적음")
                .price(20000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(100)
                .salesCount(50)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product2, product1));
        when(inventoryRepository.findAll()).thenReturn(List.of(
                testInventory,
                Inventory.builder().id(2L).productId(2L).stock(100).reservedStock(0).lowStockThreshold(10).createdAt(LocalDateTime.now()).build()
        ));

        // when
        PagedResult<ProductResponse> result = productService.getPopularProducts(0, 10, "views");

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).name()).isEqualTo("조회수 많음");
        assertThat(result.content().get(1).name()).isEqualTo("조회수 적음");
    }

    @Test
    @DisplayName("인기 상품 조회 - 판매량 기준 정렬")
    void getPopularProducts_SortBySales() {
        // given
        Product product1 = Product.builder()
                .id(1L)
                .name("판매량 많음")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(100)
                .salesCount(500)
                .createdAt(LocalDateTime.now())
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .name("판매량 적음")
                .price(20000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(1000)
                .salesCount(50)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product2, product1));
        when(inventoryRepository.findAll()).thenReturn(List.of(
                testInventory,
                Inventory.builder().id(2L).productId(2L).stock(100).reservedStock(0).lowStockThreshold(10).createdAt(LocalDateTime.now()).build()
        ));

        // when
        PagedResult<ProductResponse> result = productService.getPopularProducts(0, 10, "sales");

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).name()).isEqualTo("판매량 많음");
        assertThat(result.content().get(1).name()).isEqualTo("판매량 적음");
    }

    @Test
    @DisplayName("인기도 점수 계산 - 판매량 가중치 검증")
    void popularityScore_WeightedCorrectly() {
        // given
        Product productHighSales = Product.builder()
                .id(1L)
                .name("판매 중심 상품")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(50)
                .salesCount(100)
                .createdAt(LocalDateTime.now())
                .build();

        Product productHighViews = Product.builder()
                .id(2L)
                .name("조회 중심 상품")
                .price(20000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .viewCount(1000)
                .salesCount(5)
                .createdAt(LocalDateTime.now())
                .build();

        // when & then
        assertThat(productHighSales.getPopularityScore()).isEqualTo(1050);
        assertThat(productHighViews.getPopularityScore()).isEqualTo(1050);
    }
}
