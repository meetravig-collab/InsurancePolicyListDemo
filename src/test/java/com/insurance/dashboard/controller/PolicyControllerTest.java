package com.insurance.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.dashboard.api.controller.PolicyController;
import com.insurance.dashboard.api.dto.request.FlagPoliciesRequest;
import com.insurance.dashboard.api.mapper.PolicyMapperImpl;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;
import com.insurance.dashboard.service.PolicyService;
import com.insurance.dashboard.service.PolicySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyController.class)
@Import(PolicyMapperImpl.class)
class PolicyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private PolicyService policyService;

    private Policy policy;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policy = Policy.builder()
                .id(id).policyNumber("POL-100001").policyholderName("John Smith")
                .lineOfBusiness(LineOfBusiness.PROPERTY).region(Region.SINGAPORE).status(PolicyStatus.ACTIVE)
                .underwriter("Acme Underwriting Co.")
                .premiumAmount(new BigDecimal("250000.00")).currency("SGD")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .expiryDate(LocalDate.of(2029, 1, 1))
                .flaggedForReview(false)
                .build();
    }

    @Test
    void getPolicies_returnsPaginatedList() throws Exception {
        when(policyService.getPolicies(any(), any()))
                .thenReturn(new PageResult<>(List.of(policy), 1, 0, 10));

        mockMvc.perform(get("/api/v1/policies?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].policyNumber").value("POL-100001"))
                .andExpect(jsonPath("$.content[0].policyholderName").value("John Smith"))
                .andExpect(jsonPath("$.content[0].lineOfBusiness").value("Property"))
                .andExpect(jsonPath("$.content[0].status").value("Active"))
                .andExpect(jsonPath("$.content[0].premiumAmount").value(250000.00))
                .andExpect(jsonPath("$.content[0].currency").value("SGD"))
                .andExpect(jsonPath("$.content[0].flaggedForReview").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPolicies_withStatusFilter_passesFilterToService() throws Exception {
        when(policyService.getPolicies(any(), any()))
                .thenReturn(new PageResult<>(List.of(policy), 1, 0, 10));

        mockMvc.perform(get("/api/v1/policies?status=ACTIVE"))
                .andExpect(status().isOk());

        ArgumentCaptor<PolicyFilter> captor = ArgumentCaptor.forClass(PolicyFilter.class);
        verify(policyService).getPolicies(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().status()).isEqualTo(PolicyStatus.ACTIVE);
    }

    @Test
    void getPolicies_withRegionFilter_passesFilterToService() throws Exception {
        when(policyService.getPolicies(any(), any()))
                .thenReturn(new PageResult<>(List.of(policy), 1, 0, 10));

        mockMvc.perform(get("/api/v1/policies?region=JAPAN"))
                .andExpect(status().isOk());

        ArgumentCaptor<PolicyFilter> captor = ArgumentCaptor.forClass(PolicyFilter.class);
        verify(policyService).getPolicies(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().region()).isEqualTo(Region.JAPAN);
    }

    @Test
    void getPolicyById_returnsPolicy() throws Exception {
        when(policyService.getPolicyById(id)).thenReturn(policy);

        mockMvc.perform(get("/api/v1/policies/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("POL-100001"))
                .andExpect(jsonPath("$.lineOfBusiness").value("Property"));
    }

    @Test
    void flagPoliciesForReview_returnsFlaggedCount() throws Exception {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        FlagPoliciesRequest request = new FlagPoliciesRequest(ids);
        when(policyService.flagPoliciesForReview(any())).thenReturn(2);

        mockMvc.perform(patch("/api/v1/policies/flag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flaggedCount").value(2))
                .andExpect(jsonPath("$.policyIds", hasSize(2)));
    }

    @Test
    void flagPoliciesForReview_returns400_whenPolicyIdsEmpty() throws Exception {
        FlagPoliciesRequest request = new FlagPoliciesRequest(List.of());

        mockMvc.perform(patch("/api/v1/policies/flag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSummaryStats_returnsAggregatedStats() throws Exception {
        PolicySummary summary = new PolicySummary(
                Map.of("Active", 5L, "Pending", 1L),
                Map.of("Property", new BigDecimal("850000.00")),
                1L);
        when(policyService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/policies/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiringSoonCount").value(1))
                .andExpect(jsonPath("$.countsByStatus.Active").value(5));
    }
}
