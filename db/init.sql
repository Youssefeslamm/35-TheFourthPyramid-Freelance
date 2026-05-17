CREATE DATABASE userdb;
CREATE DATABASE proposaldb;
CREATE DATABASE contractdb;
CREATE DATABASE walletdb;
CREATE DATABASE jobdb;
CREATE TYPE user_role_enum AS ENUM ('ADMIN', 'CLIENT', 'FREELANCER');
CREATE TYPE user_status_enum AS ENUM ('ACTIVE', 'DEACTIVATED');
CREATE TYPE proficiency_level_enum AS ENUM ('BEGINNER', 'INTERMEDIATE', 'EXPERT');

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

CREATE TYPE payout_method_enum AS ENUM (
    'BANK_TRANSFER',
    'PAYPAL',
    'CRYPTO'
);
CREATE TYPE payout_status_enum AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED',
    'REFUNDED'
);
CREATE TYPE discount_type_enum AS ENUM (
    'PERCENTAGE',
    'FIXED'
);
CREATE TYPE contract_status_enum AS ENUM (
    'ACTIVE', 'COMPLETED', 'TERMINATED', 'DISPUTED'
);

CREATE TYPE job_status_enum AS ENUM ('OPEN', 'IN_PROGRESS', 'CLOSED');
CREATE TYPE job_category_enum AS ENUM ('WEB_DEV', 'WRITING', 'DESIGN', 'MOBILE', 'DEVELOPMENT');
CREATE TYPE job_attachment_type_enum AS ENUM ('BRIEF', 'MOCKUP', 'REFERENCE', 'CONTRACT_TEMPLATE');

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL UNIQUE,
    role user_role_enum NOT NULL,
    status user_status_enum NOT NULL,
    preferences JSONB,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_skills (
    id BIGSERIAL PRIMARY KEY,
    skill_name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    years_of_experience INTEGER NOT NULL,
    proficiency_level proficiency_level_enum NOT NULL,
    is_primary BOOLEAN,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS jobs (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category job_category_enum NOT NULL,
    status job_status_enum NOT NULL,
    budget_min DOUBLE PRECISION NOT NULL,
    budget_max DOUBLE PRECISION NOT NULL,
    rating DOUBLE PRECISION NOT NULL,
    total_ratings INTEGER NOT NULL,
    requirements JSONB,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS job_attachments (
    id BIGSERIAL PRIMARY KEY,
    type job_attachment_type_enum NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    expiry_date DATE NOT NULL,
    verified BOOLEAN NOT NULL,
    metadata JSONB,
    uploaded_at TIMESTAMP NOT NULL,
    job_id BIGINT NOT NULL,
    CONSTRAINT fk_job_attachments_job FOREIGN KEY (job_id) REFERENCES jobs(id)
);

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

INSERT INTO contracts (
    id,
    job_id,
    freelancer_id,
    client_id,
    proposal_id,
    agreed_amount,
    status,
    start_date,
    end_date,
    metadata,
    created_at
) VALUES (
    9006,
    9002,
    1,
    1,
    9006,
    2000.0,
    'COMPLETED',
    '2026-03-01 00:00:00',
    '2026-03-15 00:00:00',
    '{}'::jsonb,
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    job_id = EXCLUDED.job_id,
    freelancer_id = EXCLUDED.freelancer_id,
    client_id = EXCLUDED.client_id,
    proposal_id = EXCLUDED.proposal_id,
    agreed_amount = EXCLUDED.agreed_amount,
    status = EXCLUDED.status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    metadata = EXCLUDED.metadata;

SELECT setval(pg_get_serial_sequence('contracts', 'id'), GREATEST((SELECT MAX(id) FROM contracts), 9006), true);

CREATE TABLE IF NOT EXISTS payouts (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    method payout_method_enum NOT NULL,
    status payout_status_enum NOT NULL,
    transaction_details JSONB,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS promo_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    discount_type discount_type_enum NOT NULL,
    discount_value DOUBLE PRECISION NOT NULL,
    max_uses INTEGER NOT NULL,
    current_uses INTEGER NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL,
    metadata JSONB
);

CREATE TABLE IF NOT EXISTS payout_promos (
    id BIGSERIAL PRIMARY KEY,
    discount_applied DOUBLE PRECISION NOT NULL,
    applied_at TIMESTAMP NOT NULL,
    payout_id BIGINT NOT NULL,
    promo_code_id BIGINT NOT NULL,
    CONSTRAINT fk_payout_promos_payout FOREIGN KEY (payout_id) REFERENCES payouts(id),
    CONSTRAINT fk_payout_promos_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id)
);
