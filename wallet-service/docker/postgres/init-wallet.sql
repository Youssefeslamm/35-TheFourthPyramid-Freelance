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
