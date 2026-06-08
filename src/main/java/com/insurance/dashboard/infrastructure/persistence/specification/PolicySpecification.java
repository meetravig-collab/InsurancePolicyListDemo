package com.insurance.dashboard.infrastructure.persistence.specification;

import com.insurance.dashboard.domain.model.Policy.LineOfBusiness;
import com.insurance.dashboard.domain.model.Policy.PolicyStatus;
import com.insurance.dashboard.domain.model.Policy.Region;
import com.insurance.dashboard.domain.query.PolicyFilter;
import com.insurance.dashboard.infrastructure.persistence.entity.PolicyEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Builds a JPA {@link Specification} from a domain {@link PolicyFilter} by composing one
 * self-contained, null-safe specification per criterion.
 *
 * <p>Open/Closed: each criterion is isolated and absent values become a no-op
 * ({@code cb.conjunction()}). Supporting a new filter means adding a new criterion method
 * and one line in {@link #fromFilter} — existing criteria are never modified.
 */
public final class PolicySpecification {

    private PolicySpecification() {}

    public static Specification<PolicyEntity> fromFilter(PolicyFilter f) {
        return Specification.allOf(
                hasStatus(f.status()),
                hasRegion(f.region()),
                hasLineOfBusiness(f.lineOfBusiness()),
                effectiveDateOnOrAfter(f.effectiveDateFrom()),
                effectiveDateOnOrBefore(f.effectiveDateTo()),
                matchesText(f.search())
        );
    }

    private static Specification<PolicyEntity> hasStatus(PolicyStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<PolicyEntity> hasRegion(Region region) {
        return (root, query, cb) ->
                region == null ? cb.conjunction() : cb.equal(root.get("region"), region);
    }

    private static Specification<PolicyEntity> hasLineOfBusiness(LineOfBusiness lineOfBusiness) {
        return (root, query, cb) ->
                lineOfBusiness == null ? cb.conjunction() : cb.equal(root.get("lineOfBusiness"), lineOfBusiness);
    }

    private static Specification<PolicyEntity> effectiveDateOnOrAfter(LocalDate from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("effectiveDate"), from);
    }

    private static Specification<PolicyEntity> effectiveDateOnOrBefore(LocalDate to) {
        return (root, query, cb) ->
                to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("effectiveDate"), to);
    }

    private static Specification<PolicyEntity> matchesText(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("policyNumber")), pattern),
                    cb.like(cb.lower(root.get("policyholderName")), pattern),
                    cb.like(cb.lower(root.get("underwriter")), pattern)
            );
        };
    }
}
