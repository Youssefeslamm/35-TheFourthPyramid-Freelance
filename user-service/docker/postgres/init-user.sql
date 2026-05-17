CREATE TYPE user_role_enum AS ENUM ('ADMIN', 'CLIENT', 'FREELANCER');
CREATE TYPE user_status_enum AS ENUM ('ACTIVE', 'DEACTIVATED');
CREATE TYPE proficiency_level_enum AS ENUM ('BEGINNER', 'INTERMEDIATE', 'EXPERT');

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
