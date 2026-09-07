package com.saswato.ecommerce.ecommerceapi.repository;

import com.saswato.ecommerce.ecommerceapi.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByActiveTrue();

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    boolean existsBySku(String sku);
}
