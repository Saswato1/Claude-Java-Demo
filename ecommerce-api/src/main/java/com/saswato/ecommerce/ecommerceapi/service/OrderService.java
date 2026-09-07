package com.saswato.ecommerce.ecommerceapi.service;

import com.saswato.ecommerce.ecommerceapi.dto.*;
import com.saswato.ecommerce.ecommerceapi.entity.*;
import com.saswato.ecommerce.ecommerceapi.enums.OrderStatus;
import com.saswato.ecommerce.ecommerceapi.enums.PaymentStatus;
import com.saswato.ecommerce.ecommerceapi.exception.InvalidOperationException;
import com.saswato.ecommerce.ecommerceapi.exception.ResourceNotFoundException;
import com.saswato.ecommerce.ecommerceapi.repository.OrderRepository;
import com.saswato.ecommerce.ecommerceapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerService customerService;
    private final ProductService productService;

    // ── Valid status transitions (one-way) ────────────────────────────────────
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING,    Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,  Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,    Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED,  Set.of(),
            OrderStatus.CANCELLED,  Set.of(),
            OrderStatus.REFUNDED,   Set.of()
    );

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public OrderResponse findByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return toResponse(order);
    }

    public List<OrderResponse> findByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Place order ──────────────────────────────────────────────────────────

    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        // 1. Validate customer
        Customer customer = customerService.getOrThrow(request.getCustomerId());

        // 2. Build order
        Order order = Order.builder()
                .customer(customer)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Process each item: validate product, check stock, reduce stock
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productService.getOrThrow(itemRequest.getProductId());

            if (!product.getActive()) {
                throw new InvalidOperationException(
                        "Product '" + product.getName() + "' is not available for purchase");
            }
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InvalidOperationException(
                        "Insufficient stock for '" + product.getName()
                                + "'. Available: " + product.getStockQuantity()
                                + ", requested: " + itemRequest.getQuantity());
            }

            // Reduce stock at order placement
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            order.addItem(item);

            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalAmount(totalAmount);

        // 4. Create payment record
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .amount(totalAmount)
                .build();
        order.setPayment(payment);

        // 5. Save order (cascades to items and payment)
        return toResponse(orderRepository.save(order));
    }

    // ── Update status ────────────────────────────────────────────────────────

    @Transactional
    public OrderResponse updateStatus(Long id, StatusUpdateRequest request) {
        Order order = getOrThrow(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.getStatus();

        // Handle cancellation separately — it has side-effects
        if (target == OrderStatus.CANCELLED) {
            return toResponse(cancelOrder(order));
        }

        // Validate transition
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidOperationException(
                    "Cannot transition order from " + current + " to " + target);
        }

        order.setStatus(target);
        return toResponse(orderRepository.save(order));
    }

    // ── Cancel order ─────────────────────────────────────────────────────────

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        return toResponse(cancelOrder(getOrThrow(id)));
    }

    private Order cancelOrder(Order order) {
        OrderStatus current = order.getStatus();
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(OrderStatus.CANCELLED)) {
            throw new InvalidOperationException(
                    "Cannot cancel order in status " + current);
        }

        // Restore stock for every item
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // If payment was completed, mark it REFUNDED
        Payment payment = order.getPayment();
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Order order = getOrThrow(id);
        orderRepository.delete(order);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Order getOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            paymentResponse = PaymentResponse.builder()
                    .id(p.getId())
                    .paymentMethod(p.getPaymentMethod().name())
                    .paymentStatus(p.getPaymentStatus().name())
                    .amount(p.getAmount())
                    .createdAt(p.getCreatedAt())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .items(itemResponses)
                .payment(paymentResponse)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
