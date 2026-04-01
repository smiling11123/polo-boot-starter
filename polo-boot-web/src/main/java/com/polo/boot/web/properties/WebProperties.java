package com.polo.boot.web.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.web")
public class WebProperties {
    /**
     * 是否启用 Web 模块自动装配。
     */
    private boolean enabled = true;

    /**
     * 是否启用全局异常处理器。
     */
    private boolean exceptionHandlerEnabled = true;

    /**
     * 是否启用操作日志切面。
     */
    private boolean operationLogEnabled = true;
}
