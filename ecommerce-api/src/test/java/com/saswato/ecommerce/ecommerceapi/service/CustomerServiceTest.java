package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.CustomerRequest;
import com.saswato.ecommerce.ecommerceapi.dto.CustomerResponse;
import com.saswato.ecommerce.ecommerceapi.entity.Customer;
import com.saswato.ecommerce.ecommerceapi.exception.InvalidOperationException;
import com.saswato.ecommerce.ecommerceapi.exception.ResourceNotFoundException;
import com.saswato.ecommerce.ecommerceapi.repository.CustomerRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer john;
    private Customer jane;

    @BeforeEach
    void setUp() {
        john = Customer.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1-202-555-0101")
                .address("123 Maple Street")
                .createdAt(LocalDateTime.now())
                .build();

        jane = Customer.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("+1-202-555-0142")
                .address("88 Oak Avenue")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all customers")
        void returnsAllCustomers() {
            given(customerRepository.findAll()).willReturn(List.of(john, jane));

            List<CustomerResponse> result = customerService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return customer when found")
        void returnsCustomerWhenFound() {
            given(customerRepository.findById(1L)).willReturn(Optional.of(john));

            CustomerResponse result = customerService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("should throw when not found")
        void throwsWhenNotFound() {
            given(customerRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer");
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("should return customer by email")
        void returnsCustomerByEmail() {
            given(customerRepository.findByEmail("john.doe@example.com"))
                    .willReturn(Optional.of(john));

            CustomerResponse result = customerService.findByEmail("john.doe@example.com");

            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("should throw when email not found")
        void throwsWhenEmailNotFound() {
            given(customerRepository.findByEmail("unknown@example.com"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findByEmail("unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create customer successfully")
        void createsCustomer() {
            CustomerRequest request = CustomerRequest.builder()
                    .firstName("Ravi")
                    .lastName("Kumar")
                    .email("ravi.kumar@example.com")
                    .phone("+91-90000-12345")
                    .address("12 MG Road")
                    .build();

            given(customerRepository.existsByEmail("ravi.kumar@example.com")).willReturn(false);
            given(customerRepository.save(any(Customer.class))).willAnswer(invocation -> {
                Customer saved = invocation.getArgument(0);
                saved.setId(3L);
                return saved;
            });

            CustomerResponse result = customerService.create(request);

            assertThat(result.getFirstName()).isEqualTo("Ravi");
            assertThat(result.getEmail()).isEqualTo("ravi.kumar@example.com");
            then(customerRepository).should().save(any(Customer.class));
        }

        @Test
        @DisplayName("should throw InvalidOperationException for duplicate email")
        void throwsForDuplicateEmail() {
            CustomerRequest request = CustomerRequest.builder()
                    .firstName("John")
                    .lastName("Duplicate")
                    .email("john.doe@example.com")
                    .build();

            given(customerRepository.existsByEmail("john.doe@example.com")).willReturn(true);

            assertThatThrownBy(() -> customerService.create(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("already exists");

            then(customerRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update customer successfully")
        void updatesCustomer() {
            CustomerRequest request = CustomerRequest.builder()
                    .firstName("John")
                    .lastName("Updated")
                    .email("john.doe@example.com")
                    .phone("+1-999-000-0000")
                    .address("456 New Street")
                    .build();

            given(customerRepository.findById(1L)).willReturn(Optional.of(john));
            given(customerRepository.findByEmail("john.doe@example.com")).willReturn(Optional.of(john));
            given(customerRepository.save(any(Customer.class))).willAnswer(invocation -> invocation.getArgument(0));

            CustomerResponse result = customerService.update(1L, request);

            assertThat(result.getLastName()).isEqualTo("Updated");
            assertThat(result.getAddress()).isEqualTo("456 New Street");
        }

        @Test
        @DisplayName("should throw when updating to an email owned by another customer")
        void throwsForDuplicateEmailOnUpdate() {
            CustomerRequest request = CustomerRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("jane.smith@example.com")
                    .build();

            given(customerRepository.findById(1L)).willReturn(Optional.of(john));
            given(customerRepository.findByEmail("jane.smith@example.com")).willReturn(Optional.of(jane));

            assertThatThrownBy(() -> customerService.update(1L, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing customer")
        void deletesCustomer() {
            given(customerRepository.findById(1L)).willReturn(Optional.of(john));

            customerService.delete(1L);

            then(customerRepository).should().delete(john);
        }

        @Test
        @DisplayName("should throw when deleting non-existent customer")
        void throwsWhenDeletingNonExistent() {
            given(customerRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
