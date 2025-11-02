package com.hhplus.ecommerce.domain.product.service;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.domain.product.model.Inventory;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;
import com.hhplus.ecommerce.domain.product.repository.InventoryRepository;
import com.hhplus.ecommerce.domain.product.repository.ProductRepository;
import com.hhplus.ecommerce.global.common.dto.PagedResult;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    public ProductResponse getProductDetail(Long id) {
        Product product = getProduct(id);
        Inventory inventory = getInventory(id);

        return ProductResponse.of(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory().name(),
                product.getBrand(),
                product.getImageUrl(),
                product.getStatus().name(),
                inventory.getStock(),
                inventory.getReservedStock(),
                inventory.getAvailableStock(),
                product.getCreatedAt()
        );
    }

    public PagedResult<ProductResponse> getProducts(ProductCategory category, ProductStatus status, String sort, int page, int size)  {
        List<Product> products = productRepository.findAll();

        // TODO: 실제 DB 전환 시, Repository 레벨 필터링으로 개선 필요
        if (category != null || status != null) {
            products = products.stream()
                    .filter(p -> category == null || p.getCategory() == category)
                    .filter(p -> status == null || p.getStatus() == status)
                    .toList();
        }

        sortProducts(products, sort);

        Map<Long, Inventory> inventories = loadAllInventoriesAsMap();

        List<ProductResponse> responses = products.stream()
                .map(p -> {
                    Inventory inv = inventories.getOrDefault(p.getId(), Inventory.empty());
                    return ProductResponse.of(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            p.getPrice(),
                            p.getCategory().name(),
                            p.getBrand(),
                            p.getImageUrl(),
                            p.getStatus().name(),
                            inv.getStock(),
                            inv.getReservedStock(),
                            inv.getAvailableStock(),
                            p.getCreatedAt()
                    );
                })
                .toList();

        return PagedResult.of(responses, page, size);
    }

    public Inventory getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    public List<Inventory> getLowStockProducts() {
        return inventoryRepository.findLowStockProducts();
    }

    private Map<Long, Inventory> loadAllInventoriesAsMap() {
        return inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv));
    }

    private void sortProducts(List<Product> products, String sort) {
        if (sort == null || sort.isBlank()) return;

        Comparator<Product> comparator = switch (sort) {
            case "price,asc" -> Comparator.comparing(Product::getPrice);
            case "price,desc" -> Comparator.comparing(Product::getPrice).reversed();
            case "name,asc" -> Comparator.comparing(Product::getName);
            case "created,desc" -> Comparator.comparing(Product::getCreatedAt).reversed();
            default -> null;
        };

        if (comparator != null) products.sort(comparator);
    }

}
