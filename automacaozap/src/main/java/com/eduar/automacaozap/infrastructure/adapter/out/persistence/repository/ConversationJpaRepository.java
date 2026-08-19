package com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.ConversationJpaEntity;

public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    Optional<ConversationJpaEntity> findByContactId(UUID contactId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConversationJpaEntity c where c.id = :id")
    Optional<ConversationJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
