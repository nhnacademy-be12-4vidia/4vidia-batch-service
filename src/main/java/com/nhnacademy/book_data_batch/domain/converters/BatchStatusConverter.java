package com.nhnacademy.book_data_batch.domain.converters;

import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BatchStatusConverter implements AttributeConverter<BatchStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(BatchStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public BatchStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return BatchStatus.of(dbData);
    }
}
