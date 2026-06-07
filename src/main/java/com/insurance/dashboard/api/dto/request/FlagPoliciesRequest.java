package com.insurance.dashboard.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlagPoliciesRequest {
    @NotEmpty(message = "policyIds must not be empty")
    private List<UUID> policyIds;
}
