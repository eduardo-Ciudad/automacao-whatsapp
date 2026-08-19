package com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.LeadJpaEntity;

public interface LeadJpaRepository extends JpaRepository<LeadJpaEntity, UUID> {
}
