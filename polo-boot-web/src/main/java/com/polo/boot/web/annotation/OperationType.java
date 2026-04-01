package com.polo.boot.web.annotation;

public enum OperationType {
    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    QUERY("查询"),
    EXPORT("导出"),
    IMPORT("导入"),
    LOGIN("登录"),
    LOGOUT("登出"),
    AUDIT("审核"),
    OTHER("其他");

    private final String label;
    OperationType(String label) { this.label = label; }

    public String getLabel() {
        return this.label;
    }
}
