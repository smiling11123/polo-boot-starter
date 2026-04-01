package com.polo.boot.validation.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class ContentValidationRecord {
    private String detectorType;
    private String strategy;
    private String message;
    private String contentPreview;
    private String matchedDetail;
    private Integer hitCount;
    private LocalDateTime occurredAt;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
