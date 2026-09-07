package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.ProductReviewRequest;
import com.saswato.ecommerce.ecommerceapi.dto.ProductReviewResponse;
import com.saswato.ecommerce.ecommerceapi.entity.Customer;
import com.saswato.ecommerce.ecommerceapi.entity.Product;
import com.saswato.ecommerce.ecommerceapi.entity.ProductReview;
import com.saswato.ecommerce.ecommerceapi.exception.ResourceNotFoundException;
import com.saswato.ecommerce.ecommerceapi.repository.ProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private ProductReviewService productReviewService;

    private Product product;
    private Customer customer;
    private ProductReview review;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .build();

        customer = Customer.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();

        review = ProductReview.builder()
                .id(1L)
                .product(product)
                .customer(customer)
                .rating(5)
                .comment("Great product!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("addReview")
    class AddReview {

        @Test
        @DisplayName("should add review successfully")
        void addsReviewSuccessfully() {
            ProductReviewRequest request = ProductReviewRequest.builder()
                    .productId(1L)
                    .customerId(1L)
                    .rating(5)
                    .comment("Great product!")
                    .build();

            given(productService.getOrThrow(1L)).willReturn(product);
            given(customerService.getOrThrow(1L)).willReturn(customer);
            given(productReviewRepository.save(any(ProductReview.class))).willAnswer(invocation -> {
                ProductReview saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            ProductReviewResponse response = productReviewService.addReview(request);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProductId()).isEqualTo(1L);
            assertThat(response.getCustomerId()).isEqualTo(1L);
            assertThat(response.getCustomerName()).isEqualTo("John Doe");
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getComment()).isEqualTo("Great product!");

            then(productReviewRepository).should().save(any(ProductReview.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void throwsWhenProductNotFound() {
            ProductReviewRequest request = ProductReviewRequest.builder()
                    .productId(99L)
                    .customerId(1L)
                    .rating(5)
                    .build();

            given(productService.getOrThrow(99L)).willThrow(new ResourceNotFoundException("Product", "id", 99L));

            assertThatThrownBy(() -> productReviewService.addReview(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");

            then(productReviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer not found")
        void throwsWhenCustomerNotFound() {
            ProductReviewRequest request = ProductReviewRequest.builder()
                    .productId(1L)
                    .customerId(99L)
                    .rating(5)
                    .build();

            given(productService.getOrThrow(1L)).willReturn(product);
            given(customerService.getOrThrow(99L)).willThrow(new ResourceNotFoundException("Customer", "id", 99L));

            assertThatThrownBy(() -> productReviewService.addReview(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer");

            then(productReviewRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("getReviewsByProduct")
    class GetReviewsByProduct {

        @Test
        @DisplayName("should return reviews for a product")
        void returnsReviewsForProduct() {
            given(productService.getOrThrow(1L)).willReturn(product);
            given(productReviewRepository.findByProductId(1L)).willReturn(List.of(review));

            List<ProductReviewResponse> responses = productReviewService.getReviewsByProduct(1L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(0).getProductId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void throwsWhenProductNotFound() {
            given(productService.getOrThrow(99L)).willThrow(new ResourceNotFoundException("Product", "id", 99L));

            assertThatThrownBy(() -> productReviewService.getReviewsByProduct(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
