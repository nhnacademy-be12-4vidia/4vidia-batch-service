package com.nhnacademy.book_data_batch.entity.converters;

import com.nhnacademy.book_data_batch.entity.enums.StockStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StockStatusConverter implements AttributeConverter <StockStatus, Integer>{

    @Override
    public Integer convertToDatabaseColumn(StockStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public StockStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return StockStatus.of(dbData);
    }
}
