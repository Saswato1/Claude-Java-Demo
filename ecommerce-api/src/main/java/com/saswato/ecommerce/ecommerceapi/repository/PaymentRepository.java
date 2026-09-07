package com.saswato.ecommerce.ecommerceapi.repository;

import com.saswato.ecommerce.ecommerceapi.entity.Payment;
import com.saswato.ecommerce.ecommerceapi.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByPaymentStatus(PaymentStatus paymentStatus);
}
