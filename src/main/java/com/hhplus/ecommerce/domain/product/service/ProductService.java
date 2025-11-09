package com.hhplus.ecommerce.domain.product.service;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.domain.product.model.Inventory;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;
import com.hhplus.ecommerce.domain.product.repository.InventoryRepository;
import com.hhplus.ecommerce.domain.product.repository.ProductRepository;
import com.hhplus.ecommerce.global.dto.PagedResult;
import com.hhplus.ecommerce.global.exception.BusinessException;
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

    public Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    public ProductResponse getProductDetail(Long id) {
        Product product = findProductById(id);
        Inventory inventory = getInventory(id);

        product.incrementViewCount();
        productRepository.save(product);

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

        Map<Long, Inventory> inventories = loadAllInventoriesAsMap();

        var stream = products.stream();

        if (category != null) {
            stream = stream.filter(p -> p.getCategory() == category);
        }
        if (status != null) {
            stream = stream.filter(p -> p.getStatus() == status);
        }

        Comparator<Product> comparator = getComparatorForSort(sort);
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }

        List<ProductResponse> responses = stream
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

    public Map<Long, Inventory> getInventoriesAsMap(List<Long> productIds) {
        return inventoryRepository.findAll().stream()
                .filter(inv -> productIds.contains(inv.getProductId()))
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv));
    }

    public Map<Long, Product> getProductsAsMap(List<Long> productIds) {
        return productRepository.findAll().stream()
                .filter(p -> productIds.contains(p.getId()))
                .collect(Collectors.toMap(Product::getId, p -> p));
    }

    private Map<Long, Inventory> loadAllInventoriesAsMap() {
        return inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv));
    }

    public void reserveStock(Long productId, int quantity) {
        Inventory inventory = getInventory(productId);
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
    }

    public void confirmStockReservation(Long productId, int quantity) {
        Inventory inventory = getInventory(productId);
        inventory.confirmReservation(quantity);
        inventoryRepository.save(inventory);
    }

    public void releaseStockReservation(Long productId, int quantity) {
        Inventory inventory = getInventory(productId);
        inventory.releaseReservation(quantity);
        inventoryRepository.save(inventory);
    }

    public void incrementSalesCount(Long productId, int quantity) {
        Product product = findProductById(productId);
        product.incrementSalesCount(quantity);
        productRepository.save(product);
    }

    public PagedResult<ProductResponse> getPopularProducts(int page, int size, String sortBy) {
        List<Product> products = productRepository.findAll();
        Map<Long, Inventory> inventories = loadAllInventoriesAsMap();

        Comparator<Product> comparator = switch (sortBy) {
            case "views" -> Comparator.comparing(p -> p.getViewCount() == null ? 0 : p.getViewCount(), Comparator.reverseOrder());
            case "sales" -> Comparator.comparing(p -> p.getSalesCount() == null ? 0 : p.getSalesCount(), Comparator.reverseOrder());
            case "popular" -> Comparator.comparing(Product::getPopularityScore, Comparator.reverseOrder());
            default -> Comparator.comparing(Product::getPopularityScore, Comparator.reverseOrder());
        };

        List<ProductResponse> responses = products.stream()
                .sorted(comparator)
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

    private Comparator<Product> getComparatorForSort(String sort) {
        if (sort == null || sort.isBlank()) return null;

        return switch (sort) {
            case "price,asc" -> Comparator.comparing(Product::getPrice);
            case "price,desc" -> Comparator.comparing(Product::getPrice).reversed();
            case "name,asc" -> Comparator.comparing(Product::getName);
            case "created,desc" -> Comparator.comparing(Product::getCreatedAt).reversed();
            case "popular" -> Comparator.comparing(Product::getPopularityScore).reversed();
            case "views,desc" -> Comparator.comparing(p -> p.getViewCount() == null ? 0 : p.getViewCount(), Comparator.reverseOrder());
            case "sales,desc" -> Comparator.comparing(p -> p.getSalesCount() == null ? 0 : p.getSalesCount(), Comparator.reverseOrder());
            default -> null;
        };
    }

}
