package com.hhplus.ecommerce;


import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.brand.repository.BrandRepository;
import com.hhplus.ecommerce.domain.category.entity.Category;
import com.hhplus.ecommerce.domain.category.repository.CategoryRepository;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.repository.InventoryRepository;
import com.hhplus.ecommerce.domain.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitDb {
    private final InitService initService;

    @PostConstruct
    public void init() {
        initService.dbInit();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {
        private final CategoryRepository categoryRepository;
        private final BrandRepository brandRepository;
        private final ProductRepository productRepository;
        private final InventoryRepository inventoryRepository;

        public void dbInit() {
            log.info("=== 초기 데이터 생성 시작 ===");

            // 1. 카테고리 생성
            Category electronics = createCategory(null, "전자제품", "Electronics", 1, 1, null);
            Category laptop = createCategory(electronics, "노트북", "Laptop", 2, 1, null);

            // 2. 브랜드 생성
            Brand apple = createBrand("Apple", "Apple", "https://example.com/apple-logo.png", "Apple 브랜드");
            Brand samsung = createBrand("Samsung", "Samsung", "https://example.com/samsung-logo.png", "Samsung 브랜드");

            // 3. 상품 4개 생성
            Product macbookPro = createProduct(
                    laptop, apple,
                    "MacBook Pro 14 M3",
                    "Apple M3 칩 탑재, 14인치 Liquid Retina XDR 디스플레이",
                    2_490_000L,
                    "https://example.com/macbook-pro-14.jpg",
                    100, 10
            );

            Product macbookAir = createProduct(
                    laptop, apple,
                    "MacBook Air 13 M2",
                    "Apple M2 칩 탑재, 13.6인치 Liquid Retina 디스플레이",
                    1_590_000L,
                    "https://example.com/macbook-air-13.jpg",
                    150, 10
            );

            Product galaxyBook = createProduct(
                    laptop, samsung,
                    "Galaxy Book3 Pro",
                    "13세대 Intel Core i7, 14인치 AMOLED 디스플레이",
                    1_890_000L,
                    "https://example.com/galaxy-book3-pro.jpg",
                    80, 10
            );

            Product lgGram = createProduct(
                    laptop, null,
                    "LG gram 16",
                    "13세대 Intel Core i5, 16인치 WQXGA 디스플레이, 초경량 1.19kg",
                    1_690_000L,
                    "https://example.com/lg-gram-16.jpg",
                    120, 10
            );

            log.info("=== 초기 데이터 생성 완료 ===");
            log.info("생성된 상품: {}, {}, {}, {}",
                    macbookPro.getName(),
                    macbookAir.getName(),
                    galaxyBook.getName(),
                    lgGram.getName());
        }

        private Category createCategory(Category parent, String name, String nameEn,
                                        Integer level, Integer displayOrder, String imageUrl) {
            Category category = new Category(parent, name, nameEn, level, displayOrder, imageUrl);
            return categoryRepository.save(category);
        }

        private Brand createBrand(String name, String nameEn, String logoUrl, String description) {
            Brand brand = new Brand(name, nameEn, logoUrl, description);
            return brandRepository.save(brand);
        }

        private Product createProduct(Category category, Brand brand, String name, String description,
                                      Long price, String imageUrl, int stock, int lowStockThreshold) {
            Product product = new Product(category, brand, name, description, price, imageUrl);
            Product savedProduct = productRepository.save(product);

            // 재고 생성
            Inventory inventory = new Inventory(savedProduct, stock, lowStockThreshold);
            inventoryRepository.save(inventory);

            return savedProduct;
        }
    }
}


