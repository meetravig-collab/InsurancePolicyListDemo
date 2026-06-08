package com.insurance.dashboard.acceptance;

import com.insurance.dashboard.AbstractPostgresIT;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.infrastructure.persistence.entity.PolicyEntity;
import com.insurance.dashboard.infrastructure.persistence.repository.PolicyJpaRepository;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Policy List - Acceptance")
class PolicyListAcceptanceTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PolicyJpaRepository policyRepository;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        for (int i = 1; i <= 12; i++) {
            policyRepository.save(PolicyEntity.builder()
                    .policyNumber("ACC-" + String.format("%06d", i))
                    .policyholderName("John Smith")
                    .lineOfBusiness(Policy.LineOfBusiness.PROPERTY)
                    .premiumAmount(new BigDecimal("300000.00"))
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .expiryDate(LocalDate.of(2029, 1, 1))
                    .status(Policy.PolicyStatus.ACTIVE)
                    .region(Policy.Region.SINGAPORE)
                    .currency("SGD")
                    .underwriter("Acme Underwriting Co.")
                    .build());
        }

        policyRepository.save(PolicyEntity.builder()
                .policyNumber("ACC-CANCELLED-1")
                .policyholderName("Mei Tan")
                .lineOfBusiness(Policy.LineOfBusiness.CASUALTY)
                .premiumAmount(new BigDecimal("150000.00"))
                .effectiveDate(LocalDate.of(2022, 1, 1))
                .expiryDate(LocalDate.of(2023, 1, 1))
                .status(Policy.PolicyStatus.CANCELLED)
                .region(Policy.Region.THAILAND)
                .currency("THB")
                .underwriter("Beta Risk Partners")
                .build());

        policyRepository.save(PolicyEntity.builder()
                .policyNumber("ACC-EXPIRING-1")
                .policyholderName("Hiro Tanaka")
                .lineOfBusiness(Policy.LineOfBusiness.MARINE)
                .premiumAmount(new BigDecimal("200000.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .expiryDate(LocalDate.now().plusDays(15))
                .status(Policy.PolicyStatus.ACTIVE)
                .region(Policy.Region.JAPAN)
                .currency("JPY")
                .underwriter("Acme Underwriting Co.")
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("page=0&size=10 returns at most 10 records")
    void givenPageSize10_thenAtMost10Records() throws Exception {
        mockMvc.perform(get("/api/v1/policies?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(10))));
    }

    @Test
    @DisplayName("response includes totalElements and totalPages")
    void givenPageSize10_thenPaginationMetadataPresent() throws Exception {
        mockMvc.perform(get("/api/v1/policies?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("sort by premiumAmount desc orders results")
    void givenSortByPremiumDesc_thenOrdered() throws Exception {
        // scope to this test's PROPERTY policies (ACC-0*) which all share premium 300000
        mockMvc.perform(get("/api/v1/policies?search=ACC-0&sort=premiumAmount,desc&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].premiumAmount").value(300000.00));
    }

    @Test
    @DisplayName("each policy has all schema fields")
    void givenPoliciesExist_thenAllFieldsPresent() throws Exception {
        mockMvc.perform(get("/api/v1/policies?status=ACTIVE&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].policyNumber", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].policyholderName", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].lineOfBusiness", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].status", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].premiumAmount", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].currency", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].effectiveDate", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].expiryDate", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].region", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].underwriter", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].flaggedForReview", everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[*].createdAt", everyItem(notNullValue())));
    }

    @Test
    @DisplayName("status ACTIVE renders as 'Active'")
    void givenStatusActive_thenRendersActive() throws Exception {
        mockMvc.perform(get("/api/v1/policies?status=ACTIVE&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("Active"))));
    }

    @Test
    @DisplayName("status CANCELLED renders as 'Cancelled'")
    void givenStatusCancelled_thenRendersCancelled() throws Exception {
        mockMvc.perform(get("/api/v1/policies?status=CANCELLED&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-CANCELLED-1')].status",
                        hasItem("Cancelled")));
    }

    @Test
    @DisplayName("lineOfBusiness renders as display name")
    void givenPolicies_thenLineOfBusinessIsDisplayName() throws Exception {
        mockMvc.perform(get("/api/v1/policies?lineOfBusiness=MARINE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].lineOfBusiness", everyItem(is("Marine"))));
    }

    @Test
    @DisplayName("filter by region returns matching policies")
    void givenRegionFilter_thenMatchingReturned() throws Exception {
        mockMvc.perform(get("/api/v1/policies?region=THAILAND&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].region", everyItem(is("Thailand"))));
    }

    @Test
    @DisplayName("effective date range filter excludes out-of-range policies")
    void givenEffectiveDateRange_thenMatchingReturned() throws Exception {
        mockMvc.perform(get("/api/v1/policies?effectiveDateFrom=2024-01-01&effectiveDateTo=2024-12-31&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-000001')]").exists())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-CANCELLED-1')]").doesNotExist());
    }

    @Test
    @DisplayName("search on policyholderName returns matching policies")
    void givenSearchOnName_thenMatchingReturned() throws Exception {
        mockMvc.perform(get("/api/v1/policies?search=Hiro&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-EXPIRING-1')]").exists());
    }

    @Test
    @DisplayName("search on underwriter returns matching policies")
    void givenSearchOnUnderwriter_thenMatchingReturned() throws Exception {
        mockMvc.perform(get("/api/v1/policies?search=Beta&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-CANCELLED-1')]").exists());
    }

    @Test
    @DisplayName("GET /{id} returns a single policy")
    void givenValidId_thenSinglePolicyReturned() throws Exception {
        UUID id = policyRepository.findAll().stream()
                .filter(p -> "ACC-000001".equals(p.getPolicyNumber()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/v1/policies/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("ACC-000001"))
                .andExpect(jsonPath("$.underwriter").value("Acme Underwriting Co."));
    }

    @Test
    @DisplayName("GET /{id} returns 404 for unknown id")
    void givenUnknownId_thenReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/policies/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("isExpiringSoon true when expiry within 30 days")
    void givenExpiryWithin30Days_thenExpiringSoonTrue() throws Exception {
        mockMvc.perform(get("/api/v1/policies?status=ACTIVE&page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.policyNumber == 'ACC-EXPIRING-1')].isExpiringSoon",
                        hasItem(true)));
    }

    @Test
    @DisplayName("PATCH /flag sets flaggedForReview to true")
    void givenIds_whenFlagged_thenFlaggedTrue() throws Exception {
        UUID id = policyRepository.findAll().stream()
                .filter(p -> "ACC-000001".equals(p.getPolicyNumber()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/policies/flag")
                        .contentType("application/json")
                        .content("{\"policyIds\":[\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flaggedCount").value(1));

        entityManager.clear();

        mockMvc.perform(get("/api/v1/policies/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flaggedForReview").value(true));
    }

    @Test
    @DisplayName("PATCH /flag with empty ids returns 400")
    void givenEmptyIds_thenReturns400() throws Exception {
        mockMvc.perform(patch("/api/v1/policies/flag")
                        .contentType("application/json")
                        .content("{\"policyIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /summary returns aggregated data")
    void getSummary_returnsAggregatedData() throws Exception {
        mockMvc.perform(get("/api/v1/policies/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countsByStatus").isMap())
                .andExpect(jsonPath("$.totalPremiumByLineOfBusiness").isMap())
                .andExpect(jsonPath("$.expiringSoonCount").isNumber());
    }

    // --- edge / negative cases ---

    @Test
    @DisplayName("Invalid status enum value returns 400 with a readable message")
    void givenInvalidStatus_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/policies?status=NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("status")));
    }

    @Test
    @DisplayName("Malformed UUID in path returns 400, not 500")
    void givenMalformedId_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/policies/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Filters that match nothing return an empty page (200), not an error")
    void givenNoMatches_thenEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/policies?search=zzz-no-such-policy-zzz&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Flagging an unknown id is a no-op (flaggedCount 0), not an error")
    void givenUnknownIdToFlag_thenZeroFlagged() throws Exception {
        mockMvc.perform(patch("/api/v1/policies/flag")
                        .contentType("application/json")
                        .content("{\"policyIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flaggedCount").value(0));
    }
}
