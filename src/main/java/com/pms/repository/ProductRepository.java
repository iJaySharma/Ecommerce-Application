package com.pms.repository;

import com.pms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndEnabledTrue(Long categoryId);
    List<Product> findByEnabledTrue();
}
