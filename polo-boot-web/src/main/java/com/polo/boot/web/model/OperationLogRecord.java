package com.polo.boot.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class OperationLogRecord {

    /** 链路追踪ID */
    private String traceId;

    /** 操作模块 */
    private String module;

    /** 操作类型 */
    private String operationType;

    /** 操作描述 */
    private String description;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作人IP */
    private String operatorIp;

    /** 用户代理 */
    private String userAgent;

    /** 请求URI */
    private String requestUri;

    /** 请求方法 */
    private String requestMethod;

    /** 类方法名 */
    private String classMethod;

    /** 请求参数（JSON） */
    private String params;

    /** 响应结果（JSON） */
    private String result;

    /** 是否成功 */
    private Boolean success;

    /** 错误信息 */
    private String errorMsg;

    /** 执行耗时（毫秒） */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operationTime;

    /** 扩展字段（业务自定义） */
    private Map<String, Object> extra;
}