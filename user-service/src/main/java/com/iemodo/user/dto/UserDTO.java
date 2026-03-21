package com.iemodo.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserDTO {

    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String oauthProvider;
    private String status;
    private Instant createdAt;
}
