package com.eduar.automacaozap.infrastructure.adapter.in.web;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.ObjectMapper;

import com.eduar.automacaozap.application.port.in.InboundMessageCommand;
import com.eduar.automacaozap.application.port.in.ProcessInboundMessageUseCase;
import com.eduar.automacaozap.infrastructure.adapter.in.web.dto.MetaWebhookPayload;

@RestController
@RequestMapping("/webhook")
public class MetaWebhookController {

    private final MetaWebhookSignatureValidator signatureValidator;
    private final ProcessInboundMessageUseCase processInboundMessageUseCase;
    private final ObjectMapper objectMapper;
    private final String verifyToken;

    public MetaWebhookController(
            MetaWebhookSignatureValidator signatureValidator,
            ProcessInboundMessageUseCase processInboundMessageUseCase,
            ObjectMapper objectMapper,
            @Value("${whatsapp.webhook.verify-token}") String verifyToken
    ) {
        this.signatureValidator = signatureValidator;
        this.processInboundMessageUseCase = processInboundMessageUseCase;
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature
    ) {
        if (!signatureValidator.isValid(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        MetaWebhookPayload payload = objectMapper.readValue(rawBody, MetaWebhookPayload.class);
        extractMessage(payload).ifPresent(this::dispatch);

        return ResponseEntity.ok().build();
    }

    private java.util.Optional<ExtractedMessage> extractMessage(MetaWebhookPayload payload) {
        return payload.entry().stream()
                .flatMap(entry -> entry.changes().stream())
                .map(MetaWebhookPayload.Change::value)
                .filter(value -> value.messages() != null && !value.messages().isEmpty())
                .findFirst()
                .map(value -> {
                    MetaWebhookPayload.Message message = value.messages().get(0);
                    return new ExtractedMessage(message.from(), message.id(), message.text().body());
                });
    }

    private void dispatch(ExtractedMessage extracted) {
        InboundMessageCommand command = new InboundMessageCommand(
                extracted.from(),
                extracted.metaMessageId(),
                extracted.text(),
                objectMapper.createObjectNode()
        );
        processInboundMessageUseCase.handle(command);
    }

    private record ExtractedMessage(String from, String metaMessageId, String text) {}
}
