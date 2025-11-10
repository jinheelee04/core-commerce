package com.hhplus.ecommerce.domain.brand.repository;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByName(String name);

    List<Brand> findByIsActiveTrue();

    boolean existsByName(String name);
}
