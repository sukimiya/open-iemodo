package com.iemodo.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Request to create a new tenant.
 */
@Data
public class CreateTenantRequest {

    @NotBlank(message = "Tenant ID is required")
    @Size(min = 3, max = 50, message = "Tenant ID must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tenant ID must contain only lowercase letters, numbers, and hyphens")
    private String tenantId;

    @NotBlank(message = "Tenant name is required")
    @Size(max = 200, message = "Tenant name must not exceed 200 characters")
    private String tenantName;

    @NotBlank(message = "Tenant code is required")
    @Size(min = 3, max = 100, message = "Tenant code must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tenant code must contain only lowercase letters, numbers, and hyphens")
    private String tenantCode;

    @Size(max = 50, message = "Plan type must not exceed 50 characters")
    private String planType = "STANDARD";

    @Email(message = "Invalid contact email format")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 30, message = "Contact phone must not exceed 30 characters")
    private String contactPhone;

    /** Initial configurations for the tenant */
    private Map<String, String> initialConfigs;
}
