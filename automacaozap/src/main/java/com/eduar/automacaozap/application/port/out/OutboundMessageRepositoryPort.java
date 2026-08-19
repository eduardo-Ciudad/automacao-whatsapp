package com.eduar.automacaozap.application.port.out;

import java.util.List;

import com.eduar.automacaozap.domain.model.OutboundMessage;

public interface OutboundMessageRepositoryPort {

    OutboundMessage save(OutboundMessage outboundMessage);

    List<OutboundMessage> findPendingBatch(int limit);
}
