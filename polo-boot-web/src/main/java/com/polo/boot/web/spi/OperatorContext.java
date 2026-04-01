package com.polo.boot.web.spi;

public record OperatorContext(Long operatorId, String operatorName) {
    public static OperatorContext anonymous() {
        return new OperatorContext(null, "anonymous");
    }
}
