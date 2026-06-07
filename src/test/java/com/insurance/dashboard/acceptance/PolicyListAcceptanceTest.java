package com.insurance.dashboard.acceptance;

import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.PolicyHolder;
import com.insurance.dashboard.infrastructure.persistence.repository.PolicyHolderRepository;
import com.insurance.dashboard.infrastructure.persistence.repository.PolicyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Policy List - Basic Retrieval")
class PolicyListAcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private PolicyHolderRepository policyHolderRepository;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        PolicyHolder holder = policyHolderRepository.save(PolicyHolder.builder()
                .firstName("John").lastName("Smith")
                .email("acceptance.john@test.com")
                .build());

        for (int i = 1; i <= 12; i++) {
            policyRepository.save(Policy.builder()
                    .policyNumber("ACC-2024-" + String.format("%03d", i))
                    .policyType(Policy.PolicyType.LIFE)
                    .premiumAmount(new BigDecimal("300.00"))
                    .coverageAmount(new BigDecimal("500000.00"))
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2029, 1, 1))
                    .status(Policy.PolicyStatus.ACTIVE)
                    .region(Policy.Region.SINGAPORE)
                    .currency("SGD")
                    .policyHolder(holder)
                    .build());
        }

        policyRepository.save(Policy.builder()
                .policyNumber("ACC-LAPSED-001")
                .policyType(Policy.PolicyType.HEALTH)
                .premiumAmount(new BigDecimal("150.00"))
                .coverageAmount(new BigDecimal("100000.00"))
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2023, 1, 1))
                .status(Policy.PolicyStatus.LAPSED)
                .region(Policy.Region.SINGAPORE)
                .currency("SGD")
                .policyHolder(holder)
                .build());

        policyRepository.save(Policy.builder()
                .policyNumber("ACC-EXPIRING-001")
                .policyType(Policy.PolicyType.LIFE)
                .premiumAmount(new BigDecimal("200.00"))
                .coverageAmount(new BigDecimal("200000.00"))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.now().plusDays(15))
                .status(Policy.PolicyStatus.ACTIVE)
                .region(Policy.Region.SINGAPORE)
                .currency("SGD")
                .policyHolder(holder)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Given page=0&size=10, response contains at most 10 records")
    void givenPageSize10_thenResponseContainsAtMost10Records() throws Exception {
        mockMvc.perform(get("/api/policies?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(10))));
    }

    @Test
    @DisplayName("Given page=0&size=10, response includes totalElements and totalPages")
    void givenPageSize10_thenResponseIncludesPaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/policies?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("Each policy contains: policyNumber, holderName, region, status, premium with currency, startDate, endDate")
    void givenPoliciesExist_thenEachPolicyHasAllRequiredFields() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].policyNumber", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].holderName", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].region", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].status", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].premium.amount", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].premium.currency", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].startDate", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].endDate", everyItem(notNullValue())));
    }

    @Test
    @DisplayName("holderName is the holder's full name (first + last)")
    void givenPoliciesExist_thenHolderNameIsFullName() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-2024-001')].holderName",
                        hasItem("John Smith")));
    }

    @Test
    @DisplayName("region is returned as human-readable display name, not enum value")
    void givenPoliciesExist_thenRegionIsDisplayName() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-2024-001')].region",
                        hasItem("Singapore")));
    }

    @Test
    @DisplayName("Given a policy with status ACTIVE, the frontend receives 'Active' not 'ACTIVE'")
    void givenPolicyWithStatusActive_thenFrontendReceivesActive() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("Active"))));
    }

    @Test
    @DisplayName("Given a policy with status LAPSED, the frontend receives 'Lapsed' not 'LAPSED'")
    void givenPolicyWithStatusLapsed_thenFrontendReceivesLapsed() throws Exception {
        mockMvc.perform(get("/api/policies?status=LAPSED&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-LAPSED-001')].status",
                        hasItem("Lapsed")));
    }

    @Test
    @DisplayName("premium includes both amount and currency for each policy")
    void givenPoliciesExist_thenPremiumHasAmountAndCurrency() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-2024-001')].premium.amount",
                        hasItem(300.0)))
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-2024-001')].premium.currency",
                        hasItem("SGD")));
    }

    @Test
    @DisplayName("policy dates are returned in ISO-8601 format (yyyy-MM-dd)")
    void givenPoliciesExist_thenDatesAreIso8601() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-2024-001')].startDate",
                        hasItem("2024-01-01")))
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-2024-001')].endDate",
                        hasItem("2029-01-01")));
    }

    @Test
    @DisplayName("Given a policy whose end date is within 30 days from today, then isExpiringSoon is true")
    void givenPolicyEndDateWithin30Days_thenIsExpiringSoonIsTrue() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-EXPIRING-001')].isExpiringSoon",
                        hasItem(true)));
    }

    @Test
    @DisplayName("Given a policy whose end date is beyond 30 days from today, then isExpiringSoon is false")
    void givenPolicyEndDateBeyond30Days_thenIsExpiringSoonIsFalse() throws Exception {
        mockMvc.perform(get("/api/policies?status=ACTIVE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-2024-001')].isExpiringSoon",
                        hasItem(false)));
    }
}
