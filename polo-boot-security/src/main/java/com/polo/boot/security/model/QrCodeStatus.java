package com.polo.boot.security.model;

public enum QrCodeStatus {
    WAITING_SCAN("0", "等待扫码"),
    SCANNED("1", "已扫码，待确认"),
    CONFIRMED("2", "已确认登录"),
    EXPIRED("3", "二维码已过期");

    private final String code;
    private final String desc;

    QrCodeStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
