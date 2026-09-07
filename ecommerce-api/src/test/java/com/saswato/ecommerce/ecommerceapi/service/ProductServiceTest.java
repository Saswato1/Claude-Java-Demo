package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.ProductRequest;
import com.saswato.ecommerce.ecommerceapi.dto.ProductResponse;
import com.saswato.ecommerce.ecommerceapi.entity.Category;
import com.saswato.ecommerce.ecommerceapi.entity.Product;
import com.saswato.ecommerce.ecommerceapi.exception.InvalidOperationException;
import com.saswato.ecommerce.ecommerceapi.exception.ResourceNotFoundException;
import com.saswato.ecommerce.ecommerceapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    private Category electronics;
    private Product mouse;
    private Product keyboard;

    @BeforeEach
    void setUp() {
        electronics = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .build();

        mouse = Product.builder()
                .id(1L)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("24.99"))
                .sku("ELEC-MOU-001")
                .stockQuantity(150)
                .active(true)
                .category(electronics)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        keyboard = Product.builder()
                .id(2L)
                .name("Mechanical Keyboard")
                .description("RGB keyboard")
                .price(new BigDecimal("79.99"))
                .sku("ELEC-KEY-002")
                .stockQuantity(80)
                .active(true)
                .category(electronics)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all products")
        void returnsAllProducts() {
            given(productRepository.findAll()).willReturn(List.of(mouse, keyboard));

            List<ProductResponse> result = productService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSku()).isEqualTo("ELEC-MOU-001");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return product when found")
        void returnsProductWhenFound() {
            given(productRepository.findById(1L)).willReturn(Optional.of(mouse));

            ProductResponse result = productService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Wireless Mouse");
            assertThat(result.getCategoryName()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("should throw when not found")
        void throwsWhenNotFound() {
            given(productRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }
    }

    @Nested
    @DisplayName("findByCategory")
    class FindByCategory {

        @Test
        @DisplayName("should return products for a category")
        void returnsProductsByCategory() {
            given(productRepository.findByCategoryId(1L)).willReturn(List.of(mouse, keyboard));

            List<ProductResponse> result = productService.findByCategory(1L);

            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(p -> assertThat(p.getCategoryId()).isEqualTo(1L));
        }
    }

    @Nested
    @DisplayName("findActive")
    class FindActive {

        @Test
        @DisplayName("should return only active products")
        void returnsActiveProducts() {
            given(productRepository.findByActiveTrue()).willReturn(List.of(mouse));

            List<ProductResponse> result = productService.findActive();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create product successfully")
        void createsProduct() {
            ProductRequest request = ProductRequest.builder()
                    .name("USB Hub")
                    .description("7-in-1 adapter")
                    .price(new BigDecimal("39.99"))
                    .sku("ELEC-HUB-003")
                    .stockQuantity(60)
                    .active(true)
                    .categoryId(1L)
                    .build();

            given(productRepository.existsBySku("ELEC-HUB-003")).willReturn(false);
            given(categoryService.getOrThrow(1L)).willReturn(electronics);
            given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
                Product saved = invocation.getArgument(0);
                saved.setId(3L);
                return saved;
            });

            ProductResponse result = productService.create(request);

            assertThat(result.getName()).isEqualTo("USB Hub");
            assertThat(result.getSku()).isEqualTo("ELEC-HUB-003");
            then(productRepository).should().save(any(Product.class));
        }

        @Test
        @DisplayName("should throw InvalidOperationException for duplicate SKU")
        void throwsForDuplicateSku() {
            ProductRequest request = ProductRequest.builder()
                    .name("Another Mouse")
                    .price(new BigDecimal("19.99"))
                    .sku("ELEC-MOU-001")
                    .stockQuantity(10)
                    .categoryId(1L)
                    .build();

            given(productRepository.existsBySku("ELEC-MOU-001")).willReturn(true);

            assertThatThrownBy(() -> productService.create(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("SKU")
                    .hasMessageContaining("already exists");

            then(productRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update product successfully")
        void updatesProduct() {
            ProductRequest request = ProductRequest.builder()
                    .name("Wireless Mouse v2")
                    .description("Updated mouse")
                    .price(new BigDecimal("29.99"))
                    .sku("ELEC-MOU-001")
                    .stockQuantity(200)
                    .active(true)
                    .categoryId(1L)
                    .build();

            given(productRepository.findById(1L)).willReturn(Optional.of(mouse));
            given(productRepository.findBySku("ELEC-MOU-001")).willReturn(Optional.of(mouse));
            given(categoryService.getOrThrow(1L)).willReturn(electronics);
            given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

            ProductResponse result = productService.update(1L, request);

            assertThat(result.getName()).isEqualTo("Wireless Mouse v2");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing product")
        void deletesProduct() {
            given(productRepository.findById(1L)).willReturn(Optional.of(mouse));

            productService.delete(1L);

            then(productRepository).should().delete(mouse);
        }
    }
}
