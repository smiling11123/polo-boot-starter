package com.polo.boot.web.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultRecorder implements LogRecorder {

    private final ObjectMapper objectMapper;

    public DefaultRecorder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 确保支持 Java 8 日期时间类型
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void record(OperationLogRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            log.info("[OperationLog] {}", json);
        } catch (Exception e) {
            log.error("[OperationLog] 日志序列化失败", e);
        }
    }
}
