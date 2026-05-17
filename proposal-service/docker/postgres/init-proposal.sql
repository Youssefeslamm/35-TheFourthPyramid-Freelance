CREATE TYPE proposal_status_enum AS ENUM (
    'SUBMITTED',
    'SHORTLISTED',
    'ACCEPTED',
    'REJECTED',
    'WITHDRAWN',
    'COMPLETING',
    'PAYMENT_PENDING',
    'PAID',
    'PAYMENT_FAILED',
    'REFUNDED'
);
CREATE TYPE milestone_status_enum AS ENUM ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'APPROVED');

CREATE TABLE IF NOT EXISTS proposals (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    contract_id BIGINT,
    cover_letter TEXT NOT NULL,
    bid_amount DOUBLE PRECISION NOT NULL,
    estimated_days INTEGER NOT NULL,
    status proposal_status_enum NOT NULL,
    metadata JSONB,
    submitted_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proposal_milestones (
    id BIGSERIAL PRIMARY KEY,
    milestone_order INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    status milestone_status_enum NOT NULL,
    metadata JSONB,
    proposal_id BIGINT NOT NULL,
    CONSTRAINT fk_proposal_milestones_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id)
);
