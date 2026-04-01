package com.polo.boot.validation.model;

import lombok.Data;

import java.util.List;

@Data
public class AiApiResponse {

    private String requestId;
    private List<Result> results;

    @Data
    public static class Result {
        private String scene;       // 检测场景：antispam, politics, porn
        private String label;       // 标签：normal, spam, politics, porn
        private double probability; // 置信度 0-1
        private List<Detail> details;
    }

    @Data
    public static class Detail {
        private String label;
        private double probability;
        private String text;
    }
}