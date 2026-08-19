package com.eduar.automacaozap.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.eduar.automacaozap.application.port.out.ContactRepositoryPort;
import com.eduar.automacaozap.domain.model.Contact;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.ContactJpaEntity;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository.ContactJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContactRepositoryAdapter implements ContactRepositoryPort {

    private final ContactJpaRepository contactJpaRepository;

    @Override
    public Optional<Contact> findByWhatsappNumber(String whatsappNumber) {
        return contactJpaRepository.findByWhatsappNumber(whatsappNumber).map(this::toDomain);
    }

    @Override
    public Contact save(Contact contact) {
        ContactJpaEntity saved = contactJpaRepository.save(toEntity(contact));
        return toDomain(saved);
    }

    private Contact toDomain(ContactJpaEntity entity) {
        return Contact.builder()
                .id(entity.getId())
                .whatsappNumber(entity.getWhatsappNumber())
                .name(entity.getName())
                .companyId(entity.getCompanyId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ContactJpaEntity toEntity(Contact contact) {
        return ContactJpaEntity.builder()
                .id(contact.getId())
                .whatsappNumber(contact.getWhatsappNumber())
                .name(contact.getName())
                .companyId(contact.getCompanyId())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }
}
