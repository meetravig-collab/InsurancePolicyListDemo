-- Baseline schema for the policies table.
-- Matches the JPA mapping (PolicyEntity) so Hibernate ddl-auto=validate passes.

CREATE TABLE policies (
    id                 uuid                        NOT NULL,
    policy_number      varchar(255)                NOT NULL,
    policyholder_name  varchar(255),
    line_of_business   varchar(255),
    status             varchar(255),
    premium_amount     numeric(38, 2),
    currency           varchar(255),
    effective_date     date,
    expiry_date        date,
    region             varchar(255),
    underwriter        varchar(255),
    flagged_for_review boolean DEFAULT false       NOT NULL,
    created_at         timestamp(6) with time zone,
    updated_at         timestamp(6) with time zone,
    CONSTRAINT policies_pkey PRIMARY KEY (id),
    CONSTRAINT uk_policies_policy_number UNIQUE (policy_number),
    CONSTRAINT policies_line_of_business_check
        CHECK (line_of_business IN ('PROPERTY', 'CASUALTY', 'ACCIDENT_AND_HEALTH', 'MARINE')),
    CONSTRAINT policies_region_check
        CHECK (region IN ('SINGAPORE', 'HONG_KONG', 'AUSTRALIA', 'JAPAN', 'THAILAND', 'INDONESIA', 'MALAYSIA', 'PHILIPPINES')),
    CONSTRAINT policies_status_check
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'PENDING', 'CANCELLED'))
);
