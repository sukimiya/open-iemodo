package com.iemodo.rma.dto;

import com.iemodo.rma.domain.RmaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRmaRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private RmaType type;

    /**
     * Logical region code for policy lookup (EU, US, JP …).
     * If omitted, the service resolves it from the order's shipping address.
     */
    private String regionCode;

    @NotNull
    private String reason;

    private String description;

    @NotEmpty
    @Valid
    private List<RmaItemRequest> items;

    @Data
    public static class RmaItemRequest {

        @NotNull
        private Long orderItemId;

        @NotNull
        private Long productId;

        @NotNull
        private String sku;

        @NotNull
        @Positive
        private Integer quantity;

        @NotNull
        @Positive
        private BigDecimal unitPrice;

        private String reason;
    }
}
