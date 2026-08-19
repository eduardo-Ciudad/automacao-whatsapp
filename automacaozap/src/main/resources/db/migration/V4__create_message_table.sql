CREATE TABLE message (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  UUID NOT NULL REFERENCES conversation(id),
    direction        VARCHAR(3) NOT NULL CHECK (direction IN ('IN','OUT')),
    meta_message_id  VARCHAR(100) NULL,
    content_type     VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    body             TEXT NULL,
    raw_payload      JSONB NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_conversation ON message (conversation_id);

CREATE UNIQUE INDEX idx_message_meta_id_in ON message (meta_message_id)
    WHERE direction = 'IN' AND meta_message_id IS NOT NULL;
