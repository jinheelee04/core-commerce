package com.hhplus.ecommerce.domain.cart.service;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.cart.dto.CartResponse;
import com.hhplus.ecommerce.domain.cart.entity.Cart;
import com.hhplus.ecommerce.domain.cart.entity.CartItem;
import com.hhplus.ecommerce.domain.cart.repository.CartItemRepository;
import com.hhplus.ecommerce.domain.cart.repository.CartRepository;
import com.hhplus.ecommerce.domain.category.entity.Category;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService 단위 테스트")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
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
    private User mockUser;
    private Product mockProduct;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        userId = 1L;
        productId = 100L;
        cartId = 10L;

        // Create mock entities
        mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        Category mockCategory = mock(Category.class);
        Brand mockBrand = mock(Brand.class);

        mockProduct = new Product(mockCategory, mockBrand, "테스트 상품", "설명", 10000L, null);
        setId(mockProduct, productId);

        cart = new Cart(mockUser);
        setId(cart, cartId);

        cartItem = new CartItem(cart, mockProduct, 2);
        setId(cartItem, 1000L);

        // Cart의 items에 cartItem 추가
        addItemToCart(cart, cartItem);
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

    private void addItemToCart(Cart cart, CartItem item) {
        try {
            java.lang.reflect.Field itemsField = Cart.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<CartItem> items = (List<CartItem>) itemsField.get(cart);
            items.add(item);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("기존 카트가 있으면 해당 카트를 반환한다")
    void getOrCreateCart_ExistingCart() {
        // Given
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(productService.getProductsAsMap(anyList())).thenReturn(Map.of(productId, mockProduct));

        // When
        CartResponse result = cartService.getCart(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.cartId()).isEqualTo(cartId);
        assertThat(result.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("카트가 없으면 빈 카트를 반환한다")
    void getOrCreateCart_NewCart() {
        // Given
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.empty());

        // When
        CartResponse result = cartService.getCart(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.cartId()).isNull();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("장바구니 조회 시 상품 정보가 포함된다")
    void getCart_WithItems() {
        // Given
        when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
        when(productService.getProductsAsMap(anyList())).thenReturn(Map.of(productId, mockProduct));

        // When
        CartResponse result = cartService.getCart(userId);

        // Then
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalQuantity()).isEqualTo(2);
    }
}
