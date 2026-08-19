package com.eduar.automacaozap.application.port.out;

import java.util.Optional;

import com.eduar.automacaozap.domain.model.Contact;

public interface ContactRepositoryPort {

    Optional<Contact> findByWhatsappNumber(String whatsappNumber);

    Contact save(Contact contact);
}
