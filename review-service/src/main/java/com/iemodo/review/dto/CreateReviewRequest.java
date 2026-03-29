package com.iemodo.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateReviewRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private Long orderItemId;

    @NotNull
    private Long productId;

    private Long skuId;

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 200)
    private String title;

    @Size(min = 5, max = 2000)
    private String content;

    /** Optional: URLs of images/videos uploaded via file-service. */
    private List<String> mediaUrls;
}
