package com.eduar.automacaozap.application.port.in;

import tools.jackson.databind.JsonNode;

public record InboundMessageCommand(
        String whatsappNumber,
        String metaMessageId,
        String text,
        JsonNode rawPayload
) {
}
