package com.hhplus.ecommerce.domain.cart.service;

import com.hhplus.ecommerce.domain.cart.dto.CartItemAddResponse;
import com.hhplus.ecommerce.domain.cart.dto.CartItemResponse;
import com.hhplus.ecommerce.domain.cart.dto.CartResponse;
import com.hhplus.ecommerce.domain.cart.entity.Cart;
import com.hhplus.ecommerce.domain.cart.entity.CartItem;
import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.domain.cart.repository.CartItemRepository;
import com.hhplus.ecommerce.domain.cart.repository.CartRepository;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.domain.user.repository.UserRepository;
import com.hhplus.ecommerce.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));
    }

    @Transactional
    public CartItemAddResponse addItem(Long userId, Long productId, int quantity) {
        Product product = productService.findProductById(productId);
        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .map(existingItem -> mergeWithExistingItem(existingItem, quantity))
                .orElseGet(() -> createNewCartItem(cart, product, quantity));

        return toCartItemAddResponse(cartItem, product);
    }

    @Transactional
    public CartItemAddResponse updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem cartItem = findCartItemById(cartItemId);
        Cart cart = findCartById(cartItem.getCart().getId());

        validateCartOwnership(cart, userId);

        cartItem.updateQuantity(quantity);
        CartItem updatedItem = cartItemRepository.save(cartItem);

        return toCartItemAddResponse(updatedItem, updatedItem.getProduct());
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        CartItem cartItem = findCartItemById(cartItemId);
        Cart cart = findCartById(cartItem.getCart().getId());

        validateCartOwnership(cart, userId);

        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
            if (items.isEmpty()) {
                return;
            }
            cartItemRepository.deleteByCartId(cart.getId());
        });
    }

    public List<CartItem> getCartItemsByIds(Long userId, List<Long> cartItemIds) {
        return cartRepository.findByUserId(userId)
                .map(cart -> {
                    List<CartItem> allItems = cartItemRepository.findByCartId(cart.getId());
                    return allItems.stream()
                            .filter(item -> cartItemIds.contains(item.getId()))
                            .toList();
                })
                .orElse(List.of());
    }

    @Transactional
    public void removeCartItems(List<Long> cartItemIds) {
        for (Long cartItemId : cartItemIds) {
            cartItemRepository.deleteById(cartItemId);
        }
    }

    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .map(cart -> buildCartResponse(cart, userId))
                .orElseGet(() -> buildEmptyCartResponse(userId));
    }

    private CartResponse buildCartResponse(Cart cart, Long userId) {
        List<CartItem> cartItems = cart.getItems();

        List<Long> productIds = cartItems.stream()
                .map(item -> item.getProduct().getId())
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

    @Transactional
    private Cart createCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.USER_NOT_FOUND));

        Cart cart = new Cart(user);
        return cartRepository.save(cart);
    }

    @Transactional
    private CartItem createNewCartItem(Cart cart, Product product, int quantity) {
        CartItem cartItem = new CartItem(cart, product, quantity);
        return cartItemRepository.save(cartItem);
    }

    @Transactional
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

    private CartItemAddResponse toCartItemAddResponse(CartItem cartItem, Product product) {
        return CartItemAddResponse.of(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                product.getName(),
                cartItem.getQuantity(),
                product.getPrice() * cartItem.getQuantity(),
                cartItem.getUpdatedAt()
        );
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem, Map<Long, Product> productMap, Map<Long, Inventory> inventoryMap) {
        Product product = productMap.get(cartItem.getProduct().getId());
        Inventory inventory = inventoryMap.getOrDefault(cartItem.getProduct().getId(), new Inventory(null, 0, 0));

        return CartItemResponse.of(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                product != null ? product.getName() : "",
                product != null ? product.getImageUrl() : null,
                product != null ? product.getPrice() : 0L,
                cartItem.getQuantity(),
                product != null ? product.getPrice() * cartItem.getQuantity() : 0L,
                inventory.getAvailableStock(),
                cartItem.getCreatedAt()
        );
    }
}
