package com.eduar.automacaozap.application.port.out;

import com.eduar.automacaozap.domain.model.Lead;

public interface LeadRepositoryPort {

    Lead save(Lead lead);
}
