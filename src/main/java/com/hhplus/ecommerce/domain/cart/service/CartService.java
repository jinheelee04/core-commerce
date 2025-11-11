package com.hhplus.ecommerce.domain.cart.service;

import com.hhplus.ecommerce.domain.cart.dto.CartItemAddResponse;
import com.hhplus.ecommerce.domain.cart.dto.CartItemResponse;
import com.hhplus.ecommerce.domain.cart.dto.CartResponse;
import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.domain.cart.model.Cart;
import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.domain.cart.repository.CartItemRepository;
import com.hhplus.ecommerce.domain.cart.repository.CartRepository;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));
    }

    public CartItemAddResponse addItem(Long userId, Long productId, int quantity) {
        Product product = productService.findProductById(productId);

        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .map(existingItem -> mergeWithExistingItem(existingItem, quantity))
                .orElseGet(() -> createNewCartItem(cart.getId(), product, quantity));

        return toCartItemAddResponse(cartItem);
    }

    public CartItemAddResponse updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem cartItem = findCartItemById(cartItemId);
        Cart cart = findCartById(cartItem.getCartId());

        validateCartOwnership(cart, userId);

        cartItem.updateQuantity(quantity);
        CartItem updatedItem = cartItemRepository.save(cartItem);

        return toCartItemAddResponse(updatedItem);
    }

    public void removeItem(Long userId, Long cartItemId) {
        CartItem cartItem = findCartItemById(cartItemId);
        Cart cart = findCartById(cartItem.getCartId());

        validateCartOwnership(cart, userId);

        cartItemRepository.deleteById(cartItemId);
    }

    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
            if (items.isEmpty()) {
                return;
            }
            cartItemRepository.deleteByCartId(cart.getId());
        });
    }

    /**
     * 선택한 장바구니 항목들 조회 (주문 생성용)
     * 사용자 소유 검증 포함
     */
    public List<CartItem> getCartItemsByIds(Long userId, List<Long> cartItemIds) {
        return cartRepository.findByUserId(userId)
                .map(cart -> {
                    List<CartItem> allItems = cartItemRepository.findByCartId(cart.getId());
                    // 선택한 항목만 필터링
                    return allItems.stream()
                            .filter(item -> cartItemIds.contains(item.getId()))
                            .toList();
                })
                .orElse(List.of());
    }

    /**
     * 선택한 장바구니 항목들 삭제 (주문 완료 후)
     */
    public void removeCartItems(List<Long> cartItemIds) {
        for (Long cartItemId : cartItemIds) {
            cartItemRepository.deleteById(cartItemId);
        }
    }

    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> buildCartResponse(cart, userId))
                .orElseGet(() -> buildEmptyCartResponse(userId));
    }

    private CartResponse buildCartResponse(Cart cart, Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        Map<Long, Product> productMap = productService.getProductsAsMap(productIds);
        Map<Long, Inventory> inventoryMap = productService.getInventoriesAsMap(productIds);

        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(item -> toCartItemResponse(item, productMap, inventoryMap))
                .toList();

        return CartResponse.from(cart, itemResponses);
    }

    private CartResponse buildEmptyCartResponse(Long userId) {
        return CartResponse.empty(userId);
    }

    private Cart createCart(Long userId) {
        Cart cart = Cart.create(cartRepository.generateNextId(), userId);
        return cartRepository.save(cart);
    }

    private CartItem createNewCartItem(Long cartId, Product product, int quantity) {
        CartItem cartItem = CartItem.create(
                cartItemRepository.generateNextId(),
                cartId,
                product.getId(),
                product.getName(),
                product.getPrice(),
                quantity
        );
        return cartItemRepository.save(cartItem);
    }

    private CartItem mergeWithExistingItem(CartItem existingItem, int quantity) {
        existingItem.increaseQuantity(quantity);
        return cartItemRepository.save(existingItem);
    }

    private Cart findCartById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));
    }

    private CartItem findCartItemById(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));
    }

    private void validateCartOwnership(Cart cart, Long userId) {
        if (!cart.belongsTo(userId)) {
            throw new BusinessException(CartErrorCode.CART_NOT_FOUND);
        }
    }

    private CartItemAddResponse toCartItemAddResponse(CartItem cartItem) {
        return CartItemAddResponse.of(
                cartItem.getId(),
                cartItem.getProductId(),
                cartItem.getProductName(),
                cartItem.getQuantity(),
                cartItem.getSubtotal(),
                cartItem.getUpdatedAt()
        );
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem, Map<Long, Product> productMap, Map<Long, Inventory> inventoryMap) {
        Product product = productMap.getOrDefault(cartItem.getProductId(), null);
        Inventory inventory = inventoryMap.getOrDefault(cartItem.getProductId(), new Inventory(null, 0, 0));

        return CartItemResponse.of(
                cartItem.getId(),
                cartItem.getProductId(),
                cartItem.getProductName(),
                product != null ? product.getImageUrl() : null,
                cartItem.getProductPrice(),
                cartItem.getQuantity(),
                cartItem.getSubtotal(),
                inventory.getAvailableStock(),
                cartItem.getCreatedAt()
        );
    }
}
