package com.insurance.dashboard.service;

import com.insurance.dashboard.AbstractPostgresIT;
import com.insurance.dashboard.domain.model.Policy;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.port.PolicyRepositoryPort;
import com.insurance.dashboard.domain.query.PageQuery;
import com.insurance.dashboard.domain.query.PageResult;
import com.insurance.dashboard.domain.query.PolicyFilter;
import com.insurance.dashboard.domain.query.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies the caching layer and its invalidation strategy. Loads the full Spring
 * context (so @Cacheable/@CacheEvict proxies are active) but mocks the persistence
 * port, then asserts how many times the port is actually called.
 */
@SpringBootTest(properties = {"cache.ttl-seconds=60", "cache.max-size=1000"})
class PolicyCachingTest extends AbstractPostgresIT {

    @Autowired private PolicyService policyService;
    @Autowired private CacheManager cacheManager;
    @MockBean  private PolicyRepositoryPort port;

    private final PolicyFilter filter = new PolicyFilter(PolicyStatus.ACTIVE, null, null, null, null, null);
    private final PageQuery page = new PageQuery(0, 10, "effectiveDate", SortDirection.DESC);

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        reset(port);
    }

    @Test
    void listings_areCached_soSecondCallSkipsThePort() {
        when(port.findAll(filter, page)).thenReturn(new PageResult<>(List.of(), 0, 0, 10));

        policyService.getPolicies(filter, page);
        policyService.getPolicies(filter, page);

        verify(port, times(1)).findAll(filter, page); // second call served from cache
    }

    @Test
    void summary_isCached_soSecondCallSkipsThePort() {
        when(port.countByStatus()).thenReturn(Map.of(PolicyStatus.ACTIVE, 1L));
        when(port.totalPremiumByLineOfBusiness()).thenReturn(Map.of());
        when(port.countExpiringSoon(any(), any(), any())).thenReturn(0L);

        policyService.getSummary();
        policyService.getSummary();

        verify(port, times(1)).countByStatus(); // second call served from cache
    }

    @Test
    void flagging_evictsListingsCache_soNextListHitsThePortAgain() {
        when(port.findAll(filter, page)).thenReturn(new PageResult<>(List.of(), 0, 0, 10));
        when(port.flagForReview(any())).thenReturn(1);

        policyService.getPolicies(filter, page);                 // miss -> port (1)
        policyService.getPolicies(filter, page);                 // hit  -> cached
        policyService.flagPoliciesForReview(List.of(UUID.randomUUID())); // evicts listings
        policyService.getPolicies(filter, page);                 // miss -> port (2)

        verify(port, times(2)).findAll(filter, page);
    }
}
