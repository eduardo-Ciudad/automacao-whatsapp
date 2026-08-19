CREATE TABLE lead (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id      UUID NOT NULL REFERENCES contact(id),
    conversation_id UUID NOT NULL REFERENCES conversation(id),
    interest        VARCHAR(255) NULL,
    description     TEXT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW'
                        CHECK (status IN ('NEW','IN_PROGRESS','WON','LOST')),
    company_id      BIGINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lead_contact ON lead (contact_id);
