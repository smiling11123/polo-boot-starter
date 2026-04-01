package com.polo.boot.validation.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.validation")
public class ValidationProperties {
    /**
     * 是否启用 validation 模块自动装配。
     */
    private boolean enabled = true;

    /**
     * 内容安全相关配置。
     */
    private InputContentProperties inputContent = new InputContentProperties();

    @Data
    public static class InputContentProperties {
        /**
         * 是否启用内容安全相关 Bean。
         */
        private boolean enabled = true;

        /**
         * 默认的 AI 检测阈值。
         */
        private double defaultAiThreshold = 0.8D;

        /**
         * 敏感词词库相关配置。
         */
        private WordLibraryProperties wordLibrary = new WordLibraryProperties();

        /**
         * AI 内容检测相关配置。
         */
        private AiProperties ai = new AiProperties();
    }

    @Data
    public static class WordLibraryProperties {
        /**
         * 是否启用内置词库。
         */
        private boolean builtInEnabled = true;

        /**
         * 内置敏感词资源目录。
         */
        private String builtInPath = "sensitive-words/";

        /**
         * 是否启用动态词库加载。
         */
        private boolean dynamicEnabled = false;

        /**
         * Redis 中存放动态词库的 Hash Key。
         */
        private String redisKey = "content:security:words";

        /**
         * 动态词库刷新间隔，单位毫秒。
         */
        private long refreshInterval = 600_000L;

        /**
         * 是否启用数据库词库加载。
         */
        private boolean databaseEnabled = false;
    }

    @Data
    public static class AiProperties {
        /**
         * 是否启用 AI 内容检测。
         */
        private boolean enabled = false;

        /**
         * 第三方 AI 接口地址。
         */
        private String apiUrl;

        /**
         * 第三方 AI 接口鉴权 Key。
         */
        private String apiKey;
    }
}
