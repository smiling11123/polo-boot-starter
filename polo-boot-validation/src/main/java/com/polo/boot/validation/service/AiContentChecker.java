package com.polo.boot.validation.service;

import com.polo.boot.validation.model.AiApiResponse;
import com.polo.boot.validation.properties.ValidationProperties;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AiContentChecker {
    private final RestTemplate restTemplate;
    private final ValidationProperties properties;

    public AiContentChecker(RestTemplate restTemplate, ValidationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * AI 语义检测
     */
    public AiCheckResult check(String text, double threshold) {
        ValidationProperties.AiProperties aiProperties = properties.getInputContent().getAi();
        if (!aiProperties.isEnabled() || !StringUtils.hasText(aiProperties.getApiUrl())) {
            return AiCheckResult.safe();
        }
        try {
            // 调用第三方 AI 服务（如阿里云内容安全、百度 AI）
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.hasText(aiProperties.getApiKey())) {
                headers.set("Authorization", "Bearer " + aiProperties.getApiKey());
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            body.put("scenes", Arrays.asList("antispam", "politics", "porn", "abuse"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<AiApiResponse> response = restTemplate.postForEntity(
                    aiProperties.getApiUrl(), request, AiApiResponse.class);

            if (response.getBody() == null) {
                return AiCheckResult.safe();
            }

            return parseAiResponse(response.getBody(), threshold);

        } catch (Exception e) {
            // 失败时根据配置决定：放行或拒绝
            return AiCheckResult.safe(); // 或 .unsafe("检测服务异常");
        }
    }

    private AiCheckResult parseAiResponse(AiApiResponse response, double threshold) {
        double maxRisk = 0.0;
        String riskLabel = "";

        for (AiApiResponse.Result result : response.getResults()) {
            if (result.getProbability() > maxRisk) {
                maxRisk = result.getProbability();
                riskLabel = result.getLabel();
            }
        }

        if (maxRisk >= threshold) {
            return AiCheckResult.unsafe(riskLabel, maxRisk);
        }

        return AiCheckResult.safe();
    }

    @Data
    public static class AiCheckResult {
        private final boolean sensitive;
        private final String label;
        private final double confidence;

        public static AiCheckResult safe() {
            return new AiCheckResult(false, null, 0.0);
        }

        public static AiCheckResult unsafe(String label, double confidence) {
            return new AiCheckResult(true, label, confidence);
        }
    }
}
