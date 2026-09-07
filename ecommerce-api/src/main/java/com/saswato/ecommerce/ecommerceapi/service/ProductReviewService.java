package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.ProductReviewRequest;
import com.saswato.ecommerce.ecommerceapi.dto.ProductReviewResponse;
import com.saswato.ecommerce.ecommerceapi.entity.Customer;
import com.saswato.ecommerce.ecommerceapi.entity.Product;
import com.saswato.ecommerce.ecommerceapi.entity.ProductReview;
import com.saswato.ecommerce.ecommerceapi.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductService productService;
    private final CustomerService customerService;

    @Transactional
    public ProductReviewResponse addReview(ProductReviewRequest request) {
        Product product = productService.getOrThrow(request.getProductId());
        Customer customer = customerService.getOrThrow(request.getCustomerId());

        ProductReview review = ProductReview.builder()
                .product(product)
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return toResponse(productReviewRepository.save(review));
    }

    public List<ProductReviewResponse> getReviewsByProduct(Long productId) {
        // Validate product exists first
        productService.getOrThrow(productId);
        
        return productReviewRepository.findByProductId(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .customerId(review.getCustomer().getId())
                .customerName(review.getCustomer().getFirstName() + " " + review.getCustomer().getLastName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
