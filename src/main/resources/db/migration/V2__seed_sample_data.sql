-- Seed 220 realistic policies covering all statuses, lines of business, APAC regions,
-- currencies, and a spread of dates and premiums (1,000 - 5,000,000).
-- Requires PostgreSQL 13+ (gen_random_uuid()).

WITH base AS (
    SELECT
        seq,
        (DATE '2022-01-01' + ((random() * 1300)::int)) AS eff,
        (1 + (random() * 2)::int)                        AS term_years  -- 1..3 year terms
    FROM generate_series(1, 220) AS seq
)
INSERT INTO policies (
    id, policy_number, policyholder_name, line_of_business, status,
    premium_amount, currency, effective_date, expiry_date, region,
    underwriter, flagged_for_review, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    'POL-' || lpad((100000 + seq)::text, 6, '0'),
    (ARRAY['Wei Chen','Mei Lin','Hiro Tanaka','Yuki Sato','Arjun Patel',
           'Siti Nurhaliza','Somchai Boonmee','Budi Santoso','Maria Santos','Jin Park',
           'Aiko Yamamoto','Raj Kumar','Lim Wei Jie','Nguyen Van Anh','Dewi Lestari',
           'Charoen Wong','Putri Handayani','Kenji Nakamura','Grace Ong','Daniel Lim'])
        [1 + (seq % 20)],
    (ARRAY['PROPERTY','CASUALTY','ACCIDENT_AND_HEALTH','MARINE'])[1 + (seq % 4)],
    (ARRAY['ACTIVE','EXPIRED','PENDING','CANCELLED'])[1 + (seq % 4)],
    round((random() * 4999000 + 1000)::numeric, 2),
    (ARRAY['USD','SGD','HKD','AUD','JPY','THB'])[1 + (seq % 6)],
    eff,
    (eff + (term_years || ' year')::interval)::date,
    (ARRAY['SINGAPORE','HONG_KONG','AUSTRALIA','JAPAN','THAILAND','INDONESIA','MALAYSIA','PHILIPPINES'])
        [1 + (seq % 8)],
    (ARRAY['Acme Underwriting Co.','Beta Risk Partners','Pacific Re',
           'Orient Assurance','Summit Underwriters','Lloyd''s APAC'])[1 + (seq % 6)],
    (seq % 11 = 0),
    now(),
    now()
FROM base;

-- A few guaranteed "expiring soon" active policies (expiry within ~3 weeks of today)
UPDATE policies
SET status = 'ACTIVE',
    expiry_date = CURRENT_DATE + ((random() * 25)::int)
WHERE policy_number IN ('POL-100007', 'POL-100042', 'POL-100099', 'POL-100150', 'POL-100201');
