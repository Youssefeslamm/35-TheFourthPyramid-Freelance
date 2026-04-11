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