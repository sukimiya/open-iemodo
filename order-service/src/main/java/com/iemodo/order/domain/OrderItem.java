package com.iemodo.order.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("order_items")
public class OrderItem extends BaseEntity {
    // id is inherited from BaseEntity

    private Long orderId;
    private Long productId;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
