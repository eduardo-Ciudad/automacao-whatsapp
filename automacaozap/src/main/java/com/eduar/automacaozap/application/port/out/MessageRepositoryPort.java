package com.eduar.automacaozap.application.port.out;

import com.eduar.automacaozap.domain.model.Message;

public interface MessageRepositoryPort {

    boolean existsByMetaMessageId(String metaMessageId);

    Message save(Message message);
}
