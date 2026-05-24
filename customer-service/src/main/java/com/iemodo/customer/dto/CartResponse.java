package com.iemodo.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponse {
    private String cartId;
    private List<CartItemDTO> items;
    private int totalItems;
    private int uniqueItems;
}
