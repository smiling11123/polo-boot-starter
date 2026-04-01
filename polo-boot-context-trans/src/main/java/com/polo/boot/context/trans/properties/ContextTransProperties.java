package com.polo.boot.context.trans.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.context-trans")
public class ContextTransProperties {
    /**
     * 是否启用上下文透传模块。
     */
    private boolean enabled = true;

    /**
     * 是否自动开启 @Async 支持。
     */
    private boolean enableAsync = true;

    /**
     * 是否透传 MDC。
     */
    private boolean propagateMdc = true;

    /**
     * 是否透传当前登录用户上下文。
     */
    private boolean propagateUserContext = true;

    /**
     * 是否透传数据权限上下文。
     */
    private boolean propagateDataScope = true;

    /**
     * 是否透传 LocaleContext。
     */
    private boolean propagateLocale = true;

    /**
     * 是否透传 UploadContext。最终还会结合 polo.storage.enabled 判断。
     */
    private boolean propagateUploadContext = true;

    /**
     * 是否自动注册默认 taskExecutor / ioExecutor。
     */
    private boolean registerDefaultExecutors = true;

    /**
     * 默认业务线程池配置。
     */
    private Executor executor = new Executor();

    /**
     * 默认 IO 线程池配置。
     */
    private Executor ioExecutor = new Executor();

    @Data
    public static class Executor {
        /**
         * 核心线程数。
         */
        private int corePoolSize = 8;

        /**
         * 最大线程数。
         */
        private int maxPoolSize = 16;

        /**
         * 队列容量。
         */
        private int queueCapacity = 100;

        /**
         * 空闲线程存活秒数。
         */
        private int keepAliveSeconds = 60;

        /**
         * 线程名前缀。
         */
        private String threadNamePrefix = "polo-async-";

        /**
         * 关闭时是否等待任务执行完成。
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 关闭等待秒数。
         */
        private int awaitTerminationSeconds = 60;
    }
}
