CREATE TYPE job_status_enum AS ENUM ('OPEN', 'IN_PROGRESS', 'CLOSED');
CREATE TYPE job_category_enum AS ENUM ('WEB_DEV', 'WRITING', 'DESIGN', 'MOBILE', 'DEVELOPMENT');
CREATE TYPE job_attachment_type_enum AS ENUM ('BRIEF', 'MOCKUP', 'REFERENCE', 'CONTRACT_TEMPLATE');

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
