package com.hhplus.ecommerce.domain.product.service;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.entity.ProductStatus;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.domain.product.repository.InventoryRepository;
import com.hhplus.ecommerce.domain.product.repository.ProductRepository;
import com.hhplus.ecommerce.global.dto.PagedResult;
import com.hhplus.ecommerce.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    @Transactional
    public ProductResponse getProductDetail(Long id) {
        Product product = findProductById(id);

        product.incrementViewCount();

        return toProductResponse(product);
    }

    public PagedResult<ProductResponse> getProducts(Long categoryId, ProductStatus status, Pageable pageable) {
        Page<Product> productPage = productRepository.findByDynamicFilters(categoryId, status, pageable);

        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();
        Map<Long, Inventory> inventoryMap = getInventoriesAsMap(productIds);

        return PagedResult.from(productPage.map(product -> toProductResponse(product, inventoryMap)));
    }

    public PagedResult<ProductResponse> getPopularProducts(String sortBy, Pageable pageable) {
        Page<Product> productPage = productRepository.findPopularProducts(sortBy, pageable);

        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();
        Map<Long, Inventory> inventoryMap = getInventoriesAsMap(productIds);

        return PagedResult.from(productPage.map(product -> toProductResponse(product, inventoryMap)));
    }

    public Inventory getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    public List<Inventory> getLowStockProducts() {
        return inventoryRepository.findLowStockInventories();
    }

    public Map<Long, Inventory> getInventoriesAsMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return inventoryRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));
    }

    public Map<Long, Product> getProductsAsMap(List<Long> productIds) {
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
    }

    @Transactional
    public void reserveStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        inventory.reserve(quantity);
    }

    @Transactional
    public void confirmStockReservation(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        inventory.confirmReservation(quantity);
    }

    @Transactional
    public void releaseStockReservation(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        inventory.releaseReservation(quantity);
    }

    @Transactional
    public void incrementSalesCount(Long productId, int quantity) {
        Product product = findProductById(productId);
        product.incrementSalesCount(quantity);
    }

    private ProductResponse toProductResponse(Product product) {
        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
        return toProductResponse(product, inventory);
    }

    private ProductResponse toProductResponse(Product product, Map<Long, Inventory> inventoryMap) {
        Inventory inventory = inventoryMap.get(product.getId());
        return toProductResponse(product, inventory);
    }

    private ProductResponse toProductResponse(Product product, Inventory inventory) {
        return ProductResponse.of(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getImageUrl(),
                product.getStatus().name(),
                inventory != null ? inventory.getStock() : 0,
                inventory != null ? inventory.getReservedStock() : 0,
                inventory != null ? inventory.getAvailableStock() : 0,
                product.getCreatedAt()
        );
    }
}
