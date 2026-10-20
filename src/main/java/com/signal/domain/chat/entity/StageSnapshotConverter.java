package com.signal.domain.chat.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.domain.chat.progress.StageSnapshot;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StageSnapshotConverter implements AttributeConverter<StageSnapshot, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(StageSnapshot attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("상담 단계 상태 직렬화 실패", e);
        }
    }

    @Override
    public StageSnapshot convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, StageSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("상담 단계 상태 역직렬화 실패", e);
        }
    }
}
