package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.*;
import com.saswato.ecommerce.ecommerceapi.entity.*;
import com.saswato.ecommerce.ecommerceapi.enums.OrderStatus;
import com.saswato.ecommerce.ecommerceapi.enums.PaymentMethod;
import com.saswato.ecommerce.ecommerceapi.enums.PaymentStatus;
import com.saswato.ecommerce.ecommerceapi.exception.InvalidOperationException;
import com.saswato.ecommerce.ecommerceapi.exception.ResourceNotFoundException;
import com.saswato.ecommerce.ecommerceapi.repository.OrderRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Category electronics;
    private Product mouse;
    private Product keyboard;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        electronics = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .build();

        mouse = Product.builder()
                .id(1L)
                .name("Wireless Mouse")
                .price(new BigDecimal("24.99"))
                .sku("ELEC-MOU-001")
                .stockQuantity(150)
                .active(true)
                .category(electronics)
                .build();

        keyboard = Product.builder()
                .id(2L)
                .name("Mechanical Keyboard")
                .price(new BigDecimal("79.99"))
                .sku("ELEC-KEY-002")
                .stockQuantity(80)
                .active(true)
                .category(electronics)
                .build();
    }

    // ── Helper to build a saved order ────────────────────────────────────────

    private Order buildOrder(OrderStatus status, PaymentStatus paymentStatus) {
        Order order = Order.builder()
                .id(1L)
                .orderNumber("ORD-ABCD1234")
                .customer(customer)
                .status(status)
                .totalAmount(new BigDecimal("104.98"))
                .shippingAddress("123 Maple Street")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderItem item1 = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(mouse)
                .quantity(1)
                .unitPrice(new BigDecimal("24.99"))
                .subtotal(new BigDecimal("24.99"))
                .build();
        OrderItem item2 = OrderItem.builder()
                .id(2L)
                .order(order)
                .product(keyboard)
                .quantity(1)
                .unitPrice(new BigDecimal("79.99"))
                .subtotal(new BigDecimal("79.99"))
                .build();
        order.getItems().addAll(List.of(item1, item2));

        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(paymentStatus)
                .amount(new BigDecimal("104.98"))
                .createdAt(LocalDateTime.now())
                .build();
        order.setPayment(payment);

        return order;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all orders")
        void returnsAllOrders() {
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findAll()).willReturn(List.of(order));

            List<OrderResponse> result = orderService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrderNumber()).isEqualTo("ORD-ABCD1234");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return order when found")
        void returnsOrderWhenFound() {
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            OrderResponse result = orderService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCustomerName()).isEqualTo("John Doe");
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getPayment()).isNotNull();
        }

        @Test
        @DisplayName("should throw when not found")
        void throwsWhenNotFound() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order");
        }
    }

    @Nested
    @DisplayName("findByOrderNumber")
    class FindByOrderNumber {

        @Test
        @DisplayName("should return order by order number")
        void returnsOrderByNumber() {
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findByOrderNumber("ORD-ABCD1234"))
                    .willReturn(Optional.of(order));

            OrderResponse result = orderService.findByOrderNumber("ORD-ABCD1234");

            assertThat(result.getOrderNumber()).isEqualTo("ORD-ABCD1234");
        }
    }

    // ── Place Order ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        @DisplayName("should place order, reduce stock, and create payment")
        void placesOrderSuccessfully() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(1L)
                    .shippingAddress("123 Maple Street")
                    .notes("Please deliver by Friday")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .items(List.of(
                            new OrderItemRequest(1L, 2),
                            new OrderItemRequest(2L, 1)
                    ))
                    .build();

            given(customerService.getOrThrow(1L)).willReturn(customer);
            given(productService.getOrThrow(1L)).willReturn(mouse);
            given(productService.getOrThrow(2L)).willReturn(keyboard);
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(1L);
                saved.setOrderNumber("ORD-TEST1234");
                return saved;
            });

            OrderResponse result = orderService.placeOrder(request);

            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getPayment().getPaymentMethod()).isEqualTo("CREDIT_CARD");

            // Verify stock was reduced
            assertThat(mouse.getStockQuantity()).isEqualTo(148);     // 150 - 2
            assertThat(keyboard.getStockQuantity()).isEqualTo(79);   // 80 - 1

            then(productRepository).should(times(2)).save(any(Product.class));
            then(orderRepository).should().save(any(Order.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when customer not found")
        void throwsWhenCustomerNotFound() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(99L)
                    .shippingAddress("Some address")
                    .paymentMethod(PaymentMethod.UPI)
                    .items(List.of(new OrderItemRequest(1L, 1)))
                    .build();

            given(customerService.getOrThrow(99L))
                    .willThrow(new ResourceNotFoundException("Customer", "id", 99L));

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer");

            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidOperationException for insufficient stock")
        void throwsForInsufficientStock() {
            mouse.setStockQuantity(3);  // Only 3 in stock

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(1L)
                    .shippingAddress("123 Maple Street")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .items(List.of(new OrderItemRequest(1L, 5)))  // Requesting 5
                    .build();

            given(customerService.getOrThrow(1L)).willReturn(customer);
            given(productService.getOrThrow(1L)).willReturn(mouse);

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Insufficient stock")
                    .hasMessageContaining("Available: 3")
                    .hasMessageContaining("requested: 5");

            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw when product is inactive")
        void throwsForInactiveProduct() {
            mouse.setActive(false);

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(1L)
                    .shippingAddress("123 Maple Street")
                    .paymentMethod(PaymentMethod.WALLET)
                    .items(List.of(new OrderItemRequest(1L, 1)))
                    .build();

            given(customerService.getOrThrow(1L)).willReturn(customer);
            given(productService.getOrThrow(1L)).willReturn(mouse);

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not available");

            then(orderRepository).should(never()).save(any());
        }
    }

    // ── Update Status ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should transition PENDING → CONFIRMED")
        void pendingToConfirmed() {
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse result = orderService.updateStatus(1L,
                    new StatusUpdateRequest(OrderStatus.CONFIRMED));

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("should transition CONFIRMED → PROCESSING")
        void confirmedToProcessing() {
            Order order = buildOrder(OrderStatus.CONFIRMED, PaymentStatus.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse result = orderService.updateStatus(1L,
                    new StatusUpdateRequest(OrderStatus.PROCESSING));

            assertThat(result.getStatus()).isEqualTo("PROCESSING");
        }

        @Test
        @DisplayName("should transition SHIPPED → DELIVERED")
        void shippedToDelivered() {
            Order order = buildOrder(OrderStatus.SHIPPED, PaymentStatus.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse result = orderService.updateStatus(1L,
                    new StatusUpdateRequest(OrderStatus.DELIVERED));

            assertThat(result.getStatus()).isEqualTo("DELIVERED");
        }

        @Test
        @DisplayName("should reject invalid transition PENDING → SHIPPED")
        void rejectsInvalidTransition() {
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateStatus(1L,
                    new StatusUpdateRequest(OrderStatus.SHIPPED)))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("should reject transition from final state DELIVERED")
        void rejectsTransitionFromDelivered() {
            Order order = buildOrder(OrderStatus.DELIVERED, PaymentStatus.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateStatus(1L,
                    new StatusUpdateRequest(OrderStatus.SHIPPED)))
                    .isInstanceOf(InvalidOperationException.class);
        }

        @Test
        @DisplayName("should reject transition from final state REFUNDED")
        void rejectsTransitionFromRefunded() {
            Order order = buildOrder(OrderStatus.REFUNDED, PaymentStatus.REFUNDED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateStatus(1L,
                    new StatusUpdateRequest(OrderStatus.PENDING)))
                    .isInstanceOf(InvalidOperationException.class);
        }
    }

    // ── Cancel Order ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should cancel PENDING order and restore stock")
        void cancelsPendingOrder() {
            mouse.setStockQuantity(148);    // was reduced from 150
            keyboard.setStockQuantity(79);  // was reduced from 80
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse result = orderService.cancelOrder(1L);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            // Stock restored: each item had quantity 1
            assertThat(mouse.getStockQuantity()).isEqualTo(149);
            assertThat(keyboard.getStockQuantity()).isEqualTo(80);
        }

        @Test
        @DisplayName("should cancel PROCESSING order and mark COMPLETED payment as REFUNDED")
        void cancelsWithRefund() {
            Order order = buildOrder(OrderStatus.PROCESSING, PaymentStatus.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse result = orderService.cancelOrder(1L);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(result.getPayment().getPaymentStatus()).isEqualTo("REFUNDED");
        }

        @Test
        @DisplayName("should not mark PENDING payment as REFUNDED on cancel")
        void doesNotRefundPendingPayment() {
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse result = orderService.cancelOrder(1L);

            assertThat(result.getPayment().getPaymentStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("should throw when cancelling DELIVERED order")
        void throwsForAlreadyDelivered() {
            Order order = buildOrder(OrderStatus.DELIVERED, PaymentStatus.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Cannot cancel")
                    .hasMessageContaining("DELIVERED");

            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw when cancelling already CANCELLED order")
        void throwsForAlreadyCancelled() {
            Order order = buildOrder(OrderStatus.CANCELLED, PaymentStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Cannot cancel")
                    .hasMessageContaining("CANCELLED");

            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should cancel via updateStatus with CANCELLED target")
        void cancelsViaStatusUpdate() {
            Order order = buildOrder(OrderStatus.CONFIRMED, PaymentStatus.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse result = orderService.updateStatus(1L,
                    new StatusUpdateRequest(OrderStatus.CANCELLED));

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(result.getPayment().getPaymentStatus()).isEqualTo("REFUNDED");
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing order")
        void deletesOrder() {
            Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PENDING);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.delete(1L);

            then(orderRepository).should().delete(order);
        }
    }
}
