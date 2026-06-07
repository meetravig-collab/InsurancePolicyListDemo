package com.insurance.dashboard.infrastructure.persistence;

import com.insurance.dashboard.domain.query.PolicyFilter;
import com.insurance.dashboard.infrastructure.persistence.entity.PolicyEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a domain {@link PolicyFilter} into a JPA {@link Specification}.
 * This is the only place where filter criteria meet persistence types.
 */
public class PolicySpecification {

    private PolicySpecification() {}

    public static Specification<PolicyEntity> fromFilter(PolicyFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.region() != null) {
                predicates.add(cb.equal(root.get("region"), f.region()));
            }
            if (f.lineOfBusiness() != null) {
                predicates.add(cb.equal(root.get("lineOfBusiness"), f.lineOfBusiness()));
            }
            if (f.effectiveDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("effectiveDate"), f.effectiveDateFrom()));
            }
            if (f.effectiveDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effectiveDate"), f.effectiveDateTo()));
            }
            if (f.search() != null && !f.search().isBlank()) {
                String pattern = "%" + f.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("policyNumber")), pattern),
                        cb.like(cb.lower(root.get("policyholderName")), pattern),
                        cb.like(cb.lower(root.get("underwriter")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
