package com.saswato.ecommerce.ecommerceapi.dto;

import com.saswato.ecommerce.ecommerceapi.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusUpdateRequest {

    @NotNull(message = "New status is required")
    private OrderStatus status;
}
