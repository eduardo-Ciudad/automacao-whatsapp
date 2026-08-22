package com.eduar.automacaozap.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.eduar.automacaozap.domain.model.Flow;

public interface FlowRepositoryPort {

    Optional<Flow> findActiveById(UUID id);

    Optional<Flow> findDefaultActiveFlow(Long companyId);
}
