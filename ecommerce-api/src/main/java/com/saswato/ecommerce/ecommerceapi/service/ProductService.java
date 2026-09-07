package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.ProductRequest;
import com.saswato.ecommerce.ecommerceapi.dto.ProductResponse;
import com.saswato.ecommerce.ecommerceapi.entity.Category;
import com.saswato.ecommerce.ecommerceapi.entity.Product;
import com.saswato.ecommerce.ecommerceapi.exception.InvalidOperationException;
import com.saswato.ecommerce.ecommerceapi.exception.ResourceNotFoundException;
import com.saswato.ecommerce.ecommerceapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public List<ProductResponse> findByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> findActive() {
        return productRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new InvalidOperationException("Product with SKU '" + request.getSku() + "' already exists");
        }
        Category category = categoryService.getOrThrow(request.getCategoryId());
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku())
                .stockQuantity(request.getStockQuantity())
                .active(request.getActive() != null ? request.getActive() : true)
                .category(category)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getOrThrow(id);
        productRepository.findBySku(request.getSku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new InvalidOperationException("Product with SKU '" + request.getSku() + "' already exists");
                });
        Category category = categoryService.getOrThrow(request.getCategoryId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setStockQuantity(request.getStockQuantity());
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setCategory(category);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = getOrThrow(id);
        productRepository.delete(product);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .sku(product.getSku())
                .stockQuantity(product.getStockQuantity())
                .active(product.getActive())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
