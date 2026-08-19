CREATE TABLE flow (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    version     INT NOT NULL DEFAULT 1,
    definition  JSONB NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT true,
    company_id  BIGINT NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
