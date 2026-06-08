package com.insurance.dashboard.domain.exception;

import java.util.UUID;

/** Domain exception — raised when a policy does not exist. */
public class PolicyNotFoundException extends RuntimeException {
    public PolicyNotFoundException(UUID id) {
        super("Policy not found with id: " + id);
    }
}
