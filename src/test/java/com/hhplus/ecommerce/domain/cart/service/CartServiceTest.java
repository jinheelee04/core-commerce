package com.hhplus.ecommerce.domain.cart.service;

import com.hhplus.ecommerce.domain.cart.dto.CartItemAddResponse;
import com.hhplus.ecommerce.domain.cart.dto.CartResponse;
import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.domain.cart.model.Cart;
import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.domain.cart.repository.CartItemRepository;
import com.hhplus.ecommerce.domain.cart.repository.CartRepository;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.domain.product.model.Inventory;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService 단위 테스트")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    private Long userId;
    private Long productId;
    private Long cartId;
    private Long cartItemId;
    private Product product;
    private Inventory inventory;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        userId = 1L;
        productId = 100L;
        cartId = 10L;
        cartItemId = 1000L;

        product = Product.builder()
                .id(productId)
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .brand("TestBrand")
                .imageUrl("http://example.com/image.jpg")
                .status(ProductStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        inventory = Inventory.builder()
                .id(1L)
                .productId(productId)
                .stock(100)
                .reservedStock(0)
                .lowStockThreshold(10)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        cartItem = CartItem.builder()
                .id(cartItemId)
                .cartId(cartId)
                .productId(productId)
                .productName(product.getName())
                .productPrice(product.getPrice())
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getOrCreateCart 메서드는")
    class GetOrCreateCartTest {

        @Test
        @DisplayName("기존 카트가 있으면 해당 카트를 반환한다")
        void shouldReturnExistingCart() {
            // Given
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            // When
            Cart result = cartService.getOrCreateCart(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(cartId);
            assertThat(result.getUserId()).isEqualTo(userId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("기존 카트가 없으면 새 카트를 생성한다")
        void shouldCreateNewCart() {
            // Given
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cartRepository.generateNextId()).thenReturn(cartId);
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            // When
            Cart result = cartService.getOrCreateCart(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(cartId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartRepository, times(1)).generateNextId();
            verify(cartRepository, times(1)).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("addItem 메서드는")
    class AddItemTest {

        @Test
        @DisplayName("신규 상품을 장바구니에 추가한다")
        void shouldAddNewItemToCart() {
            // Given
            int quantity = 3;
            when(productService.getProduct(productId)).thenReturn(product);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());
            when(cartItemRepository.generateNextId()).thenReturn(cartItemId);
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

            // When
            CartItemAddResponse response = cartService.addItem(userId, productId, quantity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.quantity()).isEqualTo(cartItem.getQuantity());

            // Verify all mock interactions
            verify(productService, times(1)).getProduct(productId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartIdAndProductId(cartId, productId);
            verify(cartItemRepository, times(1)).generateNextId();
            verify(cartItemRepository, times(1)).save(any(CartItem.class));

            // Ensure cart was not created (it already existed)
            verify(cartRepository, never()).save(any(Cart.class));
            verify(cartRepository, never()).generateNextId();
        }

        @Test
        @DisplayName("기존 상품이 있으면 수량을 증가시킨다")
        void shouldIncreaseQuantityWhenItemExists() {
            // Given
            int additionalQuantity = 2;
            when(productService.getProduct(productId)).thenReturn(product);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

            // When
            CartItemAddResponse response = cartService.addItem(userId, productId, additionalQuantity);

            // Then
            assertThat(response).isNotNull();

            // Verify all mock interactions
            verify(productService, times(1)).getProduct(productId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartIdAndProductId(cartId, productId);
            verify(cartItemRepository, times(1)).save(any(CartItem.class));

            // Ensure new item was not created (existing item was updated)
            verify(cartItemRepository, never()).generateNextId();
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("품절 상품은 장바구니에 담을 수 없다")
        void shouldThrowExceptionWhenProductOutOfStock() {
            // Given
            Product outOfStockProduct = Product.builder()
                    .id(productId)
                    .name("품절 상품")
                    .price(10000L)
                    .category(ProductCategory.ELECTRONICS)
                    .status(ProductStatus.OUT_OF_STOCK)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            int quantity = 1;
            when(productService.getProduct(productId)).thenReturn(outOfStockProduct);

            // When & Then
            assertThatThrownBy(() -> cartService.addItem(userId, productId, quantity))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ProductErrorCode.PRODUCT_OUT_OF_STOCK);

            // Verify product was checked
            verify(productService, times(1)).getProduct(productId);

            // Ensure no further processing occurred after validation failure
            verify(cartRepository, never()).findByUserId(anyLong());
            verify(cartItemRepository, never()).findByCartIdAndProductId(anyLong(), anyLong());
            verify(cartItemRepository, never()).save(any(CartItem.class));
            verify(cartItemRepository, never()).generateNextId();
        }

        @Test
        @DisplayName("카트가 없으면 새로 생성 후 상품을 추가한다")
        void shouldCreateCartAndAddItem() {
            // Given
            int quantity = 1;
            when(productService.getProduct(productId)).thenReturn(product);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cartRepository.generateNextId()).thenReturn(cartId);
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());
            when(cartItemRepository.generateNextId()).thenReturn(cartItemId);
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

            // When
            CartItemAddResponse response = cartService.addItem(userId, productId, quantity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.productId()).isEqualTo(productId);

            // Verify product validation
            verify(productService, times(1)).getProduct(productId);

            // Verify cart creation flow
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartRepository, times(1)).generateNextId();
            verify(cartRepository, times(1)).save(any(Cart.class));

            // Verify cart item creation flow
            verify(cartItemRepository, times(1)).findByCartIdAndProductId(cartId, productId);
            verify(cartItemRepository, times(1)).generateNextId();
            verify(cartItemRepository, times(1)).save(any(CartItem.class));
        }
    }

    @Nested
    @DisplayName("updateItemQuantity 메서드는")
    class UpdateItemQuantityTest {

        @Test
        @DisplayName("장바구니 상품의 수량을 변경한다")
        void shouldUpdateQuantity() {
            // Given
            int newQuantity = 5;
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

            // When
            CartItemAddResponse response = cartService.updateItemQuantity(userId, cartItemId, newQuantity);

            // Then
            assertThat(response).isNotNull();

            // Verify all mock interactions
            verify(cartItemRepository, times(1)).findById(cartItemId);
            verify(cartRepository, times(1)).findById(cartId);
            verify(cartItemRepository, times(1)).save(any(CartItem.class));
        }

        @Test
        @DisplayName("다른 사용자의 카트는 수정할 수 없다")
        void shouldThrowExceptionWhenNotOwner() {
            // Given
            Long otherUserId = 999L;
            int newQuantity = 5;
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

            // When & Then
            assertThatThrownBy(() -> cartService.updateItemQuantity(otherUserId, cartItemId, newQuantity))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.CART_NOT_FOUND);

            // Verify lookups occurred before validation failure
            verify(cartItemRepository, times(1)).findById(cartItemId);
            verify(cartRepository, times(1)).findById(cartId);

            // Ensure no update occurred after validation failure
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }


        @Test
        @DisplayName("존재하지 않는 장바구니 상품은 수정할 수 없다")
        void shouldThrowExceptionWhenCartItemNotFound() {
            // Given
            int newQuantity = 5;
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.updateItemQuantity(userId, cartItemId, newQuantity))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.CART_ITEM_NOT_FOUND);

            // Verify only initial lookup occurred
            verify(cartItemRepository, times(1)).findById(cartItemId);

            // Ensure no further processing occurred
            verify(cartRepository, never()).findById(anyLong());
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }
    }

    @Nested
    @DisplayName("removeItem 메서드는")
    class RemoveItemTest {

        @Test
        @DisplayName("장바구니에서 상품을 제거한다")
        void shouldRemoveItem() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
            doNothing().when(cartItemRepository).deleteById(cartItemId);

            // When
            cartService.removeItem(userId, cartItemId);

            // Then - Verify all interactions
            verify(cartItemRepository, times(1)).findById(cartItemId);
            verify(cartRepository, times(1)).findById(cartId);
            verify(cartItemRepository, times(1)).deleteById(cartItemId);
        }

        @Test
        @DisplayName("다른 사용자의 카트 상품은 제거할 수 없다")
        void shouldThrowExceptionWhenNotOwnerOnRemove() {
            // Given
            Long otherUserId = 999L;
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

            // When & Then
            assertThatThrownBy(() -> cartService.removeItem(otherUserId, cartItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.CART_NOT_FOUND);

            // Verify lookups occurred before validation failure
            verify(cartItemRepository, times(1)).findById(cartItemId);
            verify(cartRepository, times(1)).findById(cartId);

            // Ensure no deletion occurred after validation failure
            verify(cartItemRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 상품은 제거할 수 없다")
        void shouldThrowExceptionWhenCartItemNotFoundOnRemove() {
            // Given
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.removeItem(userId, cartItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.CART_ITEM_NOT_FOUND);

            // Verify only initial lookup occurred
            verify(cartItemRepository, times(1)).findById(cartItemId);

            // Ensure no further processing occurred
            verify(cartRepository, never()).findById(anyLong());
            verify(cartItemRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("clearCart 메서드는")
    class ClearCartTest {

        @Test
        @DisplayName("장바구니의 모든 상품을 제거한다")
        void shouldClearAllItems() {
            // Given
            List<CartItem> items = List.of(cartItem);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartId(cartId)).thenReturn(items);
            doNothing().when(cartItemRepository).deleteByCartId(cartId);

            // When
            cartService.clearCart(userId);

            // Then
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartId(cartId);
            verify(cartItemRepository, times(1)).deleteByCartId(cartId);
        }

        @Test
        @DisplayName("장바구니가 없으면 아무 작업도 하지 않는다")
        void shouldDoNothingWhenCartNotFound() {
            // Given
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When
            cartService.clearCart(userId);

            // Then
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, never()).findByCartId(anyLong());
            verify(cartItemRepository, never()).deleteByCartId(anyLong());
        }

        @Test
        @DisplayName("이미 비어있는 장바구니는 삭제 작업을 수행하지 않는다")
        void shouldNotDeleteWhenCartIsAlreadyEmpty() {
            // Given
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of()); // 빈 리스트

            // When
            cartService.clearCart(userId);

            // Then
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartId(cartId);
            verify(cartItemRepository, never()).deleteByCartId(anyLong());
        }
    }

    @Nested
    @DisplayName("getCart 메서드는")
    class GetCartTest {

        @Test
        @DisplayName("장바구니 정보를 조회한다")
        void shouldReturnCartWithItems() {
            // Given
            List<CartItem> cartItems = List.of(cartItem);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartId(cartId)).thenReturn(cartItems);
            when(productService.getProductsAsMap(anyList())).thenReturn(Map.of(productId, product));
            when(productService.getInventoriesAsMap(anyList())).thenReturn(Map.of(productId, inventory));

            // When
            CartResponse response = cartService.getCart(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.cartId()).isEqualTo(cartId);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.items()).hasSize(1);
            assertThat(response.totalQuantity()).isEqualTo(cartItem.getQuantity());
            assertThat(response.totalAmount()).isEqualTo(cartItem.getSubtotal());

            // Verify all mock interactions
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartId(cartId);
            verify(productService, times(1)).getProductsAsMap(anyList());
            verify(productService, times(1)).getInventoriesAsMap(anyList());
        }

        @Test
        @DisplayName("장바구니가 비어있으면 빈 응답을 반환한다")
        void shouldReturnEmptyCartResponse() {
            // Given
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When
            CartResponse response = cartService.getCart(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.cartId()).isNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.items()).isEmpty();
            assertThat(response.totalQuantity()).isZero();
            assertThat(response.totalAmount()).isZero();

            // Verify minimal processing for empty cart
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, never()).findByCartId(anyLong());
            verify(productService, never()).getProductsAsMap(anyList());
            verify(productService, never()).getInventoriesAsMap(anyList());
        }

        @Test
        @DisplayName("여러 상품이 담긴 장바구니를 조회한다")
        void shouldReturnCartWithMultipleItems() {
            // Given
            Long productId2 = 200L;
            Product product2 = Product.builder()
                    .id(productId2)
                    .name("테스트 상품 2")
                    .price(20000L)
                    .category(ProductCategory.FASHION)
                    .status(ProductStatus.AVAILABLE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Inventory inventory2 = Inventory.builder()
                    .id(2L)
                    .productId(productId2)
                    .stock(50)
                    .reservedStock(0)
                    .build();

            CartItem cartItem2 = CartItem.builder()
                    .id(2000L)
                    .cartId(cartId)
                    .productId(productId2)
                    .productName(product2.getName())
                    .productPrice(product2.getPrice())
                    .quantity(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            List<CartItem> cartItems = List.of(cartItem, cartItem2);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartId(cartId)).thenReturn(cartItems);
            when(productService.getProductsAsMap(anyList())).thenReturn(Map.of(
                    productId, product,
                    productId2, product2
            ));
            when(productService.getInventoriesAsMap(anyList())).thenReturn(Map.of(
                    productId, inventory,
                    productId2, inventory2
            ));

            // When
            CartResponse response = cartService.getCart(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.items()).hasSize(2);
            assertThat(response.totalQuantity()).isEqualTo(cartItem.getQuantity() + cartItem2.getQuantity());
            assertThat(response.totalAmount()).isEqualTo(cartItem.getSubtotal() + cartItem2.getSubtotal());

            // Verify all mock interactions
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartId(cartId);
            verify(productService, times(1)).getProductsAsMap(anyList());
            verify(productService, times(1)).getInventoriesAsMap(anyList());
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryTest {

        @Test
        @DisplayName("수량이 1일 때 정상 처리된다")
        void shouldHandleMinimumQuantity() {
            // Given
            int quantity = 1;
            when(productService.getProduct(productId)).thenReturn(product);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());
            when(cartItemRepository.generateNextId()).thenReturn(cartItemId);
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

            // When
            CartItemAddResponse response = cartService.addItem(userId, productId, quantity);

            // Then
            assertThat(response).isNotNull();

            // Verify all interactions occurred
            verify(productService, times(1)).getProduct(productId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartIdAndProductId(cartId, productId);
            verify(cartItemRepository, times(1)).generateNextId();
            verify(cartItemRepository, times(1)).save(any(CartItem.class));
        }

        @Test
        @DisplayName("재고보다 많은 수량도 장바구니에 담을 수 있다 (주문 시점에 체크)")
        void canAddMoreThanStock() {
            // Given
            int quantity = 1000; // 재고(100)보다 많은 수량
            when(productService.getProduct(productId)).thenReturn(product);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());
            when(cartItemRepository.generateNextId()).thenReturn(cartItemId);
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

            // When
            CartItemAddResponse response = cartService.addItem(userId, productId, quantity);

            // Then
            assertThat(response).isNotNull();

            // Verify all interactions occurred (no stock validation in cart)
            verify(productService, times(1)).getProduct(productId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartIdAndProductId(cartId, productId);
            verify(cartItemRepository, times(1)).generateNextId();
            verify(cartItemRepository, times(1)).save(any(CartItem.class));
        }

        @Test
        @DisplayName("수량이 0일 때 상품 추가 시 예외가 발생한다")
        void shouldThrowExceptionWhenAddItemWithZeroQuantity() {
            // Given
            int quantity = 0;
            when(productService.getProduct(productId)).thenReturn(product);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());
            when(cartItemRepository.generateNextId()).thenReturn(cartItemId);

            // When & Then
            assertThatThrownBy(() -> cartService.addItem(userId, productId, quantity))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.INVALID_QUANTITY);

            // Verify product and cart lookups occurred
            verify(productService, times(1)).getProduct(productId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartIdAndProductId(cartId, productId);
            verify(cartItemRepository, times(1)).generateNextId();

            // Ensure no save occurred after validation failure
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("수량이 음수일 때 상품 추가 시 예외가 발생한다")
        void shouldThrowExceptionWhenAddItemWithNegativeQuantity() {
            // Given
            int quantity = -5;
            when(productService.getProduct(productId)).thenReturn(product);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());
            when(cartItemRepository.generateNextId()).thenReturn(cartItemId);

            // When & Then
            assertThatThrownBy(() -> cartService.addItem(userId, productId, quantity))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.INVALID_QUANTITY);

            // Verify product and cart lookups occurred
            verify(productService, times(1)).getProduct(productId);
            verify(cartRepository, times(1)).findByUserId(userId);
            verify(cartItemRepository, times(1)).findByCartIdAndProductId(cartId, productId);
            verify(cartItemRepository, times(1)).generateNextId();

            // Ensure no save occurred after validation failure
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("수량을 0으로 변경 시 예외가 발생한다")
        void shouldThrowExceptionWhenUpdateQuantityToZero() {
            // Given
            int newQuantity = 0;
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

            // When & Then
            assertThatThrownBy(() -> cartService.updateItemQuantity(userId, cartItemId, newQuantity))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.INVALID_QUANTITY);

            // Verify lookups occurred
            verify(cartItemRepository, times(1)).findById(cartItemId);
            verify(cartRepository, times(1)).findById(cartId);

            // Ensure no save occurred after validation failure
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("수량을 음수로 변경 시 예외가 발생한다")
        void shouldThrowExceptionWhenUpdateQuantityToNegative() {
            // Given
            int newQuantity = -3;
            when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));
            when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

            // When & Then
            assertThatThrownBy(() -> cartService.updateItemQuantity(userId, cartItemId, newQuantity))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CartErrorCode.INVALID_QUANTITY);

            // Verify lookups occurred
            verify(cartItemRepository, times(1)).findById(cartItemId);
            verify(cartRepository, times(1)).findById(cartId);

            // Ensure no save occurred after validation failure
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }
    }
}
