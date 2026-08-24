package com.eduar.automacaozap.infrastructure.adapter.in.web.dto;

import java.util.List;

public record MetaWebhookPayload(
        String object,
        List<Entry> entry
) {
    public record Entry(String id, List<Change> changes) {}

    public record Change(Value value, String field) {}

    public record Value(
            String messagingProduct,
            List<Contact> contacts,
            List<Message> messages
    ) {}

    public record Contact(String waId) {}

    public record Message(String from, String id, String timestamp, String type, Text text) {}

    public record Text(String body) {}
}
