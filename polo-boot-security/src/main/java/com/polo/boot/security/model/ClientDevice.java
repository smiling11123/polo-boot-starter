package com.polo.boot.security.model;

import lombok.Data;

@Data
public class ClientDevice implements ClientDeviceInfo{
    private String deviceId;
    private String deviceType;
    private String deviceName;
}
