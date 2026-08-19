CREATE TABLE conversation (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id            UUID NOT NULL REFERENCES contact(id),
    flow_id               UUID NULL REFERENCES flow(id),
    current_step_id       VARCHAR(100) NULL,
    context               JSONB NOT NULL DEFAULT '{}',
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                              CHECK (status IN ('ACTIVE','WAITING_HUMAN','CLOSED')),
    last_inbound_at       TIMESTAMPTZ NULL,
    last_meta_message_id  VARCHAR(100) NULL,
    company_id            BIGINT NOT NULL DEFAULT 1,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversation_contact ON conversation (contact_id);
