package com.polo.boot.security.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 支持从时间戳（毫秒）或日期字符串（yyyy-MM-dd HH:mm:ss）转换到 LocalDateTime 的反序列化器
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            // 1. 尝试解析为数字（毫秒时间戳）
            long timestamp = Long.parseLong(value);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            // 2. 尝试按照格式解析为日期字符串
            return LocalDateTime.parse(value, FORMATTER);
        }
    }
}
