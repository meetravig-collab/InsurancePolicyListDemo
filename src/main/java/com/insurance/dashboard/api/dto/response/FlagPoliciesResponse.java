package com.insurance.dashboard.api.dto.response;

import java.util.List;
import java.util.UUID;

public record FlagPoliciesResponse(int flaggedCount, List<UUID> policyIds) {}
