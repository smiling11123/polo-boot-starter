package com.polo.boot.api.doc.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "polo.api-doc")
public class OpenApiProperties {
    /**
     * 是否启用接口文档自动装配。
     */
    private boolean enabled = true;

    /**
     * 文档标题。
     */
    private String title = "Polo Boot API";

    /**
     * 文档描述。
     */
    private String description = "API documentation";

    /**
     * 文档版本号。
     */
    private String version = "v1.0.0";

    /**
     * 联系人名称。
     */
    private String contactName;

    /**
     * 联系人邮箱。
     */
    private String contactEmail;

    /**
     * 联系人主页地址。
     */
    private String contactUrl;

    /**
     * 许可证名称。
     */
    private String licenseName;

    /**
     * 许可证地址。
     */
    private String licenseUrl;

    /**
     * 文档中展示的服务地址。
     */
    private String serverUrl;

    /**
     * 服务地址说明。
     */
    private String serverDescription;

    /**
     * 是否在 Swagger UI 中显示“全部接口”分组。
     */
    private boolean showAllApisGroup = true;

    /**
     * “全部接口”分组的显示名称。
     */
    private String allApisDisplayName = "All APIs";

    /**
     * 是否在 Swagger UI 中持久化 Bearer Token，切换分组或刷新页面后仍保留授权状态。
     */
    private boolean persistAuthorization = true;

    /**
     * 是否自动创建默认分组。
     */
    private boolean createDefaultGroup = true;

    /**
     * 默认分组的内部名称。
     */
    private String defaultGroupName = "default";

    /**
     * 默认分组的显示名称。
     */
    private String defaultGroupDisplayName = "Default";

    /**
     * 文档默认扫描的控制器包路径。
     */
    private String[] packagesToScan = new String[0];

    /**
     * 默认分组匹配的路径模式。
     */
    private String[] pathsToMatch = new String[]{"/**"};

    /**
     * 默认分组需要排除的路径模式。
     */
    private String[] pathsToExclude = new String[0];

    /**
     * 是否在文档中启用 Bearer 鉴权方案。
     */
    private boolean enableSecurity = true;

    /**
     * Swagger 安全方案名称。
     */
    private String securitySchemeName = "bearerAuth";

    /**
     * Swagger 安全方案说明。
     */
    private String securitySchemeDescription = "请输入 JWT Token";

    /**
     * 不需要展示鉴权要求的接口路径。
     */
    private String[] securityExcludedPaths = new String[0];

    /**
     * 是否为接口自动补充默认错误响应。
     */
    private boolean enableDefaultResponses = true;

    /**
     * Public 分组匹配路径。
     */
    private String[] publicPath = new String[0];

    /**
     * Admin 分组匹配路径。
     */
    private String[] adminPath = new String[0];

    /**
     * Admin 分组扫描包路径。
     */
    private String[] adminPackage = new String[0];

    /**
     * Public 分组扫描包路径。
     */
    private String[] publicPackage = new String[0];

    /**
     * 自定义文档分组配置。
     */
    private List<GroupConfig> groups = new ArrayList<>();

    @Data
    public static class GroupConfig {
        /**
         * 分组内部名称，同时会用于分组接口地址。
         */
        private String name;

        /**
         * 分组显示名称。
         */
        private String displayName;

        /**
         * 当前分组匹配的路径模式。
         */
        private String[] path;

        /**
         * 当前分组需要排除的路径模式。
         */
        private String[] excludePath;

        /**
         * 当前分组扫描的包路径。
         */
        private String[] packages;

        /**
         * 分组描述。
         */
        private String description;

        /**
         * 是否启用当前分组。
         */
        private boolean enabled = true;
    }
}
