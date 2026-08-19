package com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.FlowJpaEntity;

public interface FlowJpaRepository extends JpaRepository<FlowJpaEntity, UUID> {

    Optional<FlowJpaEntity> findByIdAndActiveTrue(UUID id);
}
