package com.insurance.dashboard.acceptance;

import com.insurance.dashboard.api.controller.PolicyController;
import com.insurance.dashboard.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PolicyController.class)
@DisplayName("Policy API - Database Failure")
class PolicyDatabaseFailureAcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private PolicyService policyService;

    @Test
    @DisplayName("Given the policy database is unreachable, then the API returns 503 with a readable error message")
    void givenDatabaseUnreachable_thenReturns503WithReadableMessage() throws Exception {
        when(policyService.getPaginatedPolicies(any(), any(), any()))
                .thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "Policy service is temporarily unavailable. Please try again later."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Given the policy database is unreachable, the response does not expose internal stack traces")
    void givenDatabaseUnreachable_thenResponseDoesNotExposeStackTrace() throws Exception {
        when(policyService.getPaginatedPolicies(any(), any(), any()))
                .thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.message").value(not(containsString("CannotCreateTransactionException"))))
                .andExpect(jsonPath("$.message").value(not(containsString("at org."))));
    }
}
