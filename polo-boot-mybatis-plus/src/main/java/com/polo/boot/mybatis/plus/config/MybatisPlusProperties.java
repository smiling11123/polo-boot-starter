package com.polo.boot.mybatis.plus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.mybatis-plus")
public class MybatisPlusProperties {
    /**
     * 是否启用 MyBatis-Plus 增强模块自动装配。
     */
    private boolean enabled = true;

    /**
     * 是否启用自动填充与审计处理器。
     */
    private boolean metaObjectHandlerEnabled = true;

    /**
     * 是否启用分页拦截器。
     */
    private boolean paginationEnabled = true;

    /**
     * 是否启用乐观锁拦截器。
     */
    private boolean optimisticLockerEnabled = true;
}
