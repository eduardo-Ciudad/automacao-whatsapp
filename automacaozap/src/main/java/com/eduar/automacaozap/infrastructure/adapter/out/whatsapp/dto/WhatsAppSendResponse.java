package com.eduar.automacaozap.infrastructure.adapter.out.whatsapp.dto;

import java.util.List;

public record WhatsAppSendResponse(

        List<MessageId> messages
) {
    public record MessageId(String id) {}
}
