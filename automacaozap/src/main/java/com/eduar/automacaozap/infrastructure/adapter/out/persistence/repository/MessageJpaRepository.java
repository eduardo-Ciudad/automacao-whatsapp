package com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.MessageJpaEntity;

public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, Long> {

    boolean existsByMetaMessageId(String metaMessageId);
}
