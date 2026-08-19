CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE contact (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    whatsapp_number VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(255) NULL,
    company_id      BIGINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
