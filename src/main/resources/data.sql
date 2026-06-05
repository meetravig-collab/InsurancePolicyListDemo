-- Sample Policy Holders
INSERT INTO policy_holders (id, first_name, last_name, email, phone, date_of_birth, address)
VALUES (1, 'John', 'Smith', 'john.smith@email.com', '555-1001', '1985-03-15', '123 Main St, New York, NY');

INSERT INTO policy_holders (id, first_name, last_name, email, phone, date_of_birth, address)
VALUES (2, 'Jane', 'Doe', 'jane.doe@email.com', '555-1002', '1990-07-22', '456 Oak Ave, Los Angeles, CA');

INSERT INTO policy_holders (id, first_name, last_name, email, phone, date_of_birth, address)
VALUES (3, 'Robert', 'Johnson', 'robert.j@email.com', '555-1003', '1978-11-05', '789 Pine Rd, Chicago, IL');

-- Sample Policies
INSERT INTO policies (id, policy_number, policy_type, premium_amount, coverage_amount, start_date, end_date, status, policy_holder_id)
VALUES (1, 'POL-2024-001', 'LIFE', 250.00, 500000.00, '2024-01-01', '2029-01-01', 'ACTIVE', 1);

INSERT INTO policies (id, policy_number, policy_type, premium_amount, coverage_amount, start_date, end_date, status, policy_holder_id)
VALUES (2, 'POL-2024-002', 'HEALTH', 150.00, 100000.00, '2024-01-01', '2024-12-31', 'ACTIVE', 1);

INSERT INTO policies (id, policy_number, policy_type, premium_amount, coverage_amount, start_date, end_date, status, policy_holder_id)
VALUES (3, 'POL-2024-003', 'AUTO', 100.00, 50000.00, '2024-02-01', '2025-02-01', 'ACTIVE', 2);

INSERT INTO policies (id, policy_number, policy_type, premium_amount, coverage_amount, start_date, end_date, status, policy_holder_id)
VALUES (4, 'POL-2023-004', 'HOME', 200.00, 300000.00, '2023-06-01', '2024-06-01', 'EXPIRED', 3);
