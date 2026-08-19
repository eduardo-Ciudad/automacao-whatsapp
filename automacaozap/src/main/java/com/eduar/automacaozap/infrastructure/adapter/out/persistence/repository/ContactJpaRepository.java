package com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.ContactJpaEntity;

public interface ContactJpaRepository extends JpaRepository<ContactJpaEntity, UUID> {

    Optional<ContactJpaEntity> findByWhatsappNumber(String whatsappNumber);
}
