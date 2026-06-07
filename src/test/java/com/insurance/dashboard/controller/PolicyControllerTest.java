package com.insurance.dashboard.controller;

import com.insurance.dashboard.api.controller.PolicyController;
import com.insurance.dashboard.api.dto.response.PolicySummaryResponse;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private PolicyService policyService;

    private PolicySummaryResponse summaryResponse;

    @BeforeEach
    void setUp() {
        summaryResponse = PolicySummaryResponse.builder()
                .id(1L)
                .policyNumber("POL-001")
                .holderName("John Smith")
                .region("Singapore")
                .status("Active")
                .premium(PolicySummaryResponse.PremiumDto.builder()
                        .amount(new BigDecimal("250.00"))
                        .currency("SGD")
                        .build())
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2029, 1, 1))
                .isExpiringSoon(false)
                .build();
    }

    @Test
    void getAllPolicies_returnsPaginatedList() throws Exception {
        when(policyService.getPaginatedPolicies(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(summaryResponse), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/policies?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].policyNumber").value("POL-001"))
                .andExpect(jsonPath("$.content[0].holderName").value("John Smith"))
                .andExpect(jsonPath("$.content[0].region").value("Singapore"))
                .andExpect(jsonPath("$.content[0].status").value("Active"))
                .andExpect(jsonPath("$.content[0].premium.amount").value(250.00))
                .andExpect(jsonPath("$.content[0].premium.currency").value("SGD"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllPolicies_withStatusFilter_passesFilterToService() throws Exception {
        when(policyService.getPaginatedPolicies(eq(PolicyStatus.ACTIVE), any(), any()))
                .thenReturn(new PageImpl<>(List.of(summaryResponse)));

        mockMvc.perform(get("/api/policies?status=ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(policyService).getPaginatedPolicies(eq(PolicyStatus.ACTIVE), any(), any());
    }

    @Test
    void getAllPolicies_withRegionFilter_passesFilterToService() throws Exception {
        when(policyService.getPaginatedPolicies(any(), eq(Region.SINGAPORE), any()))
                .thenReturn(new PageImpl<>(List.of(summaryResponse)));

        mockMvc.perform(get("/api/policies?region=SINGAPORE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].region").value("Singapore"));

        verify(policyService).getPaginatedPolicies(any(), eq(Region.SINGAPORE), any());
    }

    @Test
    void getAllPolicies_isExpiringSoon_trueWhenEndDateWithin30Days() throws Exception {
        PolicySummaryResponse expiring = PolicySummaryResponse.builder()
                .id(2L).policyNumber("POL-EXP-001").holderName("Jane Doe")
                .region("Hong Kong").status("Active")
                .premium(PolicySummaryResponse.PremiumDto.builder()
                        .amount(new BigDecimal("100.00")).currency("HKD").build())
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10))
                .isExpiringSoon(true).build();

        when(policyService.getPaginatedPolicies(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(expiring)));

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isExpiringSoon").value(true));
    }

    @Test
    void getAllPolicies_returnsEmptyPage_whenNoPolicies() throws Exception {
        when(policyService.getPaginatedPolicies(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
