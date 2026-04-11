CREATE TYPE IF NOT EXISTS payout_method_enum AS ENUM (
    'BANK_TRANSFER',
    'PAYPAL',
    'CRYPTO'
);

CREATE TYPE IF NOT EXISTS payout_status_enum AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED',
    'REFUNDED'
);

CREATE TYPE IF NOT EXISTS discount_type_enum AS ENUM (
    'PERCENTAGE',
    'FIXED'
);