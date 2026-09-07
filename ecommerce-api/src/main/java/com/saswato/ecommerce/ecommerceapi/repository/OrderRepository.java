package com.saswato.ecommerce.ecommerceapi.repository;

import com.saswato.ecommerce.ecommerceapi.entity.Order;
import com.saswato.ecommerce.ecommerceapi.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
}
