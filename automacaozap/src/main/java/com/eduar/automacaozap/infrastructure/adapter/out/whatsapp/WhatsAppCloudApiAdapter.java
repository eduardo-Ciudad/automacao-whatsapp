package com.eduar.automacaozap.infrastructure.adapter.out.whatsapp;
import com.eduar.automacaozap.application.port.out.WhatsAppSenderPort;
import com.eduar.automacaozap.domain.model.OutboundMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class WhatsAppCloudApiAdapter implements WhatsAppSenderPort {


    private final RestClient restClient;
    private final String phoneNumberId;

    public WhatsAppCloudApiAdapter(
            @Value("${whatsapp.api.base-url}") String baseUrl,
            @Value("${whatsapp.api.token}") String token,
            @Value("${whatsapp.api.phone-number-id}") String phoneNumberId
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
        this.phoneNumberId = phoneNumberId;
    }

    @Override
    public String send(OutboundMessage message) {
        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "to", message.toWhatsappNumber(),
                "type", "text",
                "text", Map.of("body", extractText(message))
        );

        WhatsAppSendResponse response = restClient.post()
                .uri("/{phoneNumberId}/messages", phoneNumberId)
                .body(requestBody)
                .retrieve()
                .body(WhatsAppSendResponse.class);

        return response.messages().get(0).id();
    }

    private String extractText(OutboundMessage message) {
        return message.payload().get("text").asText();
    }
}
