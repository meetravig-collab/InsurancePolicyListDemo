package com.insurance.dashboard.common.exception;

import java.util.UUID;

public class PolicyNotFoundException extends RuntimeException {
    public PolicyNotFoundException(UUID id) {
        super("Policy not found with id: " + id);
    }
}
