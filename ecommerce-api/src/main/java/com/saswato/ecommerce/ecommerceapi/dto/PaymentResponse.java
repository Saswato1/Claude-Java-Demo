package com.saswato.ecommerce.ecommerceapi.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
