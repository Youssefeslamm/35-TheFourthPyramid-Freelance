CREATE TYPE contract_status_enum AS ENUM (
    'ACTIVE',
    'COMPLETED',
    'TERMINATED',
    'DISPUTED'
);

CREATE TABLE IF NOT EXISTS contracts (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    proposal_id BIGINT NOT NULL,
    agreed_amount DOUBLE PRECISION NOT NULL,
    status contract_status_enum NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL
);
