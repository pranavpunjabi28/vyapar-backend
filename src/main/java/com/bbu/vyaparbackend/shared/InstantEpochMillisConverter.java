package com.bbu.vyaparbackend.shared;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;

@Converter(autoApply = true)
public class InstantEpochMillisConverter implements AttributeConverter<Instant, Long> {
    @Override
    public Long convertToDatabaseColumn(Instant value) {
        return value == null ? null : value.toEpochMilli();
    }

    @Override
    public Instant convertToEntityAttribute(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }
}
