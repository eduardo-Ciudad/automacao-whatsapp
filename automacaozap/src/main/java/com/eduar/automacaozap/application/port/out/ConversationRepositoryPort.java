package com.eduar.automacaozap.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.eduar.automacaozap.domain.model.Conversation;

public interface ConversationRepositoryPort {

    Optional<Conversation> findByIdForUpdate(UUID id);

    Optional<Conversation> findByContactId(UUID contactId);

    Conversation save(Conversation conversation);
}
