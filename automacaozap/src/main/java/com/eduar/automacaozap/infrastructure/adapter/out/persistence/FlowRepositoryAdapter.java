package com.eduar.automacaozap.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eduar.automacaozap.application.port.out.FlowRepositoryPort;
import com.eduar.automacaozap.domain.model.Flow;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.FlowJpaEntity;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository.FlowJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FlowRepositoryAdapter implements FlowRepositoryPort {

    private final FlowJpaRepository flowJpaRepository;

    @Override
    public Optional<Flow> findActiveById(UUID id) {
        return flowJpaRepository.findByIdAndActiveTrue(id).map(this::toDomain);
    }

    @Override
    public Optional<Flow> findDefaultActiveFlow(Long companyId) {
        return flowJpaRepository.findFirstByCompanyIdAndActiveTrueOrderByCreatedAtDesc(companyId).map(this::toDomain);
    }

    private Flow toDomain(FlowJpaEntity entity) {
        return Flow.builder()
                .id(entity.getId())
                .name(entity.getName())
                .version(entity.getVersion())
                .definition(entity.getDefinition())
                .active(entity.isActive())
                .companyId(entity.getCompanyId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
