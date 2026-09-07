package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.CategoryRequest;
import com.saswato.ecommerce.ecommerceapi.dto.CategoryResponse;
import com.saswato.ecommerce.ecommerceapi.entity.Category;
import com.saswato.ecommerce.ecommerceapi.exception.InvalidOperationException;
import com.saswato.ecommerce.ecommerceapi.exception.ResourceNotFoundException;
import com.saswato.ecommerce.ecommerceapi.repository.CategoryRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category electronics;
    private Category books;

    @BeforeEach
    void setUp() {
        electronics = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .description("Gadgets and devices")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        books = Category.builder()
                .id(2L)
                .name("Books")
                .slug("books")
                .description("Printed books")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all categories")
        void returnsAllCategories() {
            given(categoryRepository.findAll()).willReturn(List.of(electronics, books));

            List<CategoryResponse> result = categoryService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Electronics");
            assertThat(result.get(1).getName()).isEqualTo("Books");
        }

        @Test
        @DisplayName("should return empty list when no categories exist")
        void returnsEmptyList() {
            given(categoryRepository.findAll()).willReturn(List.of());

            List<CategoryResponse> result = categoryService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return category when found")
        void returnsCategoryWhenFound() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(electronics));

            CategoryResponse result = categoryService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Electronics");
            assertThat(result.getSlug()).isEqualTo("electronics");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category")
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("findBySlug")
    class FindBySlug {

        @Test
        @DisplayName("should return category when slug exists")
        void returnsCategoryBySlug() {
            given(categoryRepository.findBySlug("electronics")).willReturn(Optional.of(electronics));

            CategoryResponse result = categoryService.findBySlug("electronics");

            assertThat(result.getSlug()).isEqualTo("electronics");
        }

        @Test
        @DisplayName("should throw when slug not found")
        void throwsWhenSlugNotFound() {
            given(categoryRepository.findBySlug("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findBySlug("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create category and auto-generate slug")
        void createsCategory() {
            CategoryRequest request = new CategoryRequest("Home & Kitchen", "Homeware");
            given(categoryRepository.existsByName("Home & Kitchen")).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> {
                Category saved = invocation.getArgument(0);
                saved.setId(3L);
                return saved;
            });

            CategoryResponse result = categoryService.create(request);

            assertThat(result.getName()).isEqualTo("Home & Kitchen");
            assertThat(result.getSlug()).isEqualTo("home-kitchen");
            then(categoryRepository).should().save(any(Category.class));
        }

        @Test
        @DisplayName("should throw InvalidOperationException for duplicate name")
        void throwsForDuplicateName() {
            CategoryRequest request = new CategoryRequest("Electronics", "Duplicate");
            given(categoryRepository.existsByName("Electronics")).willReturn(true);

            assertThatThrownBy(() -> categoryService.create(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("already exists");

            then(categoryRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update category successfully")
        void updatesCategory() {
            CategoryRequest request = new CategoryRequest("Electronics Updated", "Updated desc");
            given(categoryRepository.findById(1L)).willReturn(Optional.of(electronics));
            given(categoryRepository.findByName("Electronics Updated")).willReturn(Optional.empty());
            given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> invocation.getArgument(0));

            CategoryResponse result = categoryService.update(1L, request);

            assertThat(result.getName()).isEqualTo("Electronics Updated");
            assertThat(result.getSlug()).isEqualTo("electronics-updated");
        }

        @Test
        @DisplayName("should throw when updating to an existing name owned by another category")
        void throwsForDuplicateNameOnUpdate() {
            CategoryRequest request = new CategoryRequest("Books", "Trying to steal name");
            given(categoryRepository.findById(1L)).willReturn(Optional.of(electronics));
            given(categoryRepository.findByName("Books")).willReturn(Optional.of(books));

            assertThatThrownBy(() -> categoryService.update(1L, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should allow keeping the same name on update")
        void allowsSameNameOnUpdate() {
            CategoryRequest request = new CategoryRequest("Electronics", "New desc");
            given(categoryRepository.findById(1L)).willReturn(Optional.of(electronics));
            given(categoryRepository.findByName("Electronics")).willReturn(Optional.of(electronics));
            given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> invocation.getArgument(0));

            CategoryResponse result = categoryService.update(1L, request);

            assertThat(result.getName()).isEqualTo("Electronics");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing category")
        void deletesCategory() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(electronics));

            categoryService.delete(1L);

            then(categoryRepository).should().delete(electronics);
        }

        @Test
        @DisplayName("should throw when deleting non-existent category")
        void throwsWhenDeletingNonExistent() {
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
