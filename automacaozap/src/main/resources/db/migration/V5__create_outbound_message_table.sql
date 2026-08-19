CREATE TABLE outbound_message (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     UUID NOT NULL REFERENCES conversation(id),
    message_id          BIGINT NULL REFERENCES message(id),
    to_whatsapp_number  VARCHAR(20) NOT NULL,
    message_type        VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    payload             JSONB NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING','SENT','FAILED')),
    attempts            INT NOT NULL DEFAULT 0,
    last_error          TEXT NULL,
    meta_message_id     VARCHAR(100) NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at             TIMESTAMPTZ NULL
);

CREATE INDEX idx_outbound_pending ON outbound_message (created_at)
    WHERE status = 'PENDING';
