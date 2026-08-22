package com.eduar.automacaozap.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.eduar.automacaozap.application.port.in.InboundMessageCommand;
import com.eduar.automacaozap.application.port.in.ProcessInboundMessageUseCase;
import com.eduar.automacaozap.application.port.out.ContactRepositoryPort;
import com.eduar.automacaozap.application.port.out.ConversationRepositoryPort;
import com.eduar.automacaozap.application.port.out.FlowRepositoryPort;
import com.eduar.automacaozap.application.port.out.LeadRepositoryPort;
import com.eduar.automacaozap.application.port.out.MessageRepositoryPort;
import com.eduar.automacaozap.application.port.out.OutboundMessageRepositoryPort;
import com.eduar.automacaozap.domain.model.Contact;
import com.eduar.automacaozap.domain.model.Conversation;
import com.eduar.automacaozap.domain.model.ConversationStatus;
import com.eduar.automacaozap.domain.model.Direction;
import com.eduar.automacaozap.domain.model.Flow;
import com.eduar.automacaozap.domain.model.Lead;
import com.eduar.automacaozap.domain.model.LeadStatus;
import com.eduar.automacaozap.domain.model.Message;
import com.eduar.automacaozap.domain.model.OutboundMessage;
import com.eduar.automacaozap.domain.model.OutboundMessageType;
import com.eduar.automacaozap.domain.model.OutboundStatus;
import com.eduar.automacaozap.domain.service.FlowEngine;
import com.eduar.automacaozap.domain.service.FlowEngineResult;
import com.eduar.automacaozap.domain.service.LeadDraft;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInboundMessageService implements ProcessInboundMessageUseCase {

    private static final Long DEFAULT_COMPANY_ID = 1L;

    private final ConversationRepositoryPort conversationRepositoryPort;
    private final ContactRepositoryPort contactRepositoryPort;
    private final FlowRepositoryPort flowRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final OutboundMessageRepositoryPort outboundMessageRepositoryPort;
    private final LeadRepositoryPort leadRepositoryPort;
    private final FlowEngine flowEngine;

    @Override
    @Transactional
    public void handle(InboundMessageCommand command) {
        if (messageRepositoryPort.existsByMetaMessageId(command.metaMessageId())) {
            log.info("Evento já processado anteriormente, ignorado. metaMessageId={}", command.metaMessageId());
            return;
        }

        Contact contact = resolveContact(command.whatsappNumber());
        Conversation conversation = resolveConversation(contact);
        Flow flow = flowRepositoryPort.findActiveById(conversation.getFlowId())
                .orElseThrow(() -> new IllegalStateException("Flow não encontrado ou inativo para a conversa"));

        FlowEngineResult result = flowEngine.process(flow, conversation, command.text());

        applyResult(contact, conversation, command, result);
    }

    private Contact resolveContact(String whatsappNumber) {
        return contactRepositoryPort.findByWhatsappNumber(whatsappNumber)
                .orElseGet(() -> contactRepositoryPort.save(Contact.builder()
                        .id(UUID.randomUUID())
                        .whatsappNumber(whatsappNumber)
                        .name(null)
                        .companyId(DEFAULT_COMPANY_ID)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
    }

    // TODO: duas mensagens quase simultâneas do mesmo contato novo podem, em
    // teoria, criar duas Conversation em paralelo, pois a checagem "existe?" e a
    // criação não são atômicas sem um índice único parcial em
    // conversation(contact_id) WHERE status = 'ACTIVE'. Não implementado agora,
    // apenas documentado.
    private Conversation resolveConversation(Contact contact) {
        return conversationRepositoryPort.findByContactId(contact.getId())
                .map(existing -> conversationRepositoryPort.findByIdForUpdate(existing.getId()).orElseThrow())
                .orElseGet(() -> {
                    Flow defaultFlow = flowRepositoryPort.findDefaultActiveFlow(DEFAULT_COMPANY_ID)
                            .orElseThrow(() -> new IllegalStateException("Nenhum flow ativo configurado para a company"));

                    Conversation created = conversationRepositoryPort.save(Conversation.builder()
                            .id(UUID.randomUUID())
                            .contactId(contact.getId())
                            .flowId(defaultFlow.getId())
                            .currentStepId(null)
                            .context(JsonNodeFactory.instance.objectNode())
                            .status(ConversationStatus.ACTIVE)
                            .companyId(DEFAULT_COMPANY_ID)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build());

                    return conversationRepositoryPort.findByIdForUpdate(created.getId()).orElseThrow();
                });
    }

    private void applyResult(Contact contact, Conversation conversation, InboundMessageCommand command, FlowEngineResult result) {
        Conversation updatedConversation = Conversation.builder()
                .id(conversation.getId())
                .contactId(conversation.getContactId())
                .flowId(conversation.getFlowId())
                .currentStepId(result.nextStepId())
                .context(result.updatedContext())
                .status(result.requiresHandoff() ? ConversationStatus.WAITING_HUMAN : ConversationStatus.ACTIVE)
                .lastInboundAt(Instant.now())
                .lastMetaMessageId(command.metaMessageId())
                .companyId(conversation.getCompanyId())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
        conversationRepositoryPort.save(updatedConversation);

        messageRepositoryPort.save(Message.builder()
                .conversationId(conversation.getId())
                .direction(Direction.IN)
                .metaMessageId(command.metaMessageId())
                .contentType("TEXT")
                .body(command.text())
                .rawPayload(command.rawPayload())
                .createdAt(Instant.now())
                .build());

        for (String text : result.messagesToSend()) {
            ObjectNode payload = JsonNodeFactory.instance.objectNode();
            payload.put("text", text);

            outboundMessageRepositoryPort.save(OutboundMessage.builder()
                    .conversationId(conversation.getId())
                    .toWhatsappNumber(contact.getWhatsappNumber())
                    .type(OutboundMessageType.TEXT)
                    .payload(payload)
                    .status(OutboundStatus.PENDING)
                    .attempts(0)
                    .createdAt(Instant.now())
                    .build());
        }

        result.leadToCreate().ifPresent(leadDraft -> leadRepositoryPort.save(buildLead(contact, conversation, leadDraft)));
    }

    private Lead buildLead(Contact contact, Conversation conversation, LeadDraft leadDraft) {
        return Lead.builder()
                .id(UUID.randomUUID())
                .contactId(contact.getId())
                .conversationId(conversation.getId())
                .interest(leadDraft.interest())
                .description(leadDraft.description())
                .status(LeadStatus.NEW)
                .companyId(DEFAULT_COMPANY_ID)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
