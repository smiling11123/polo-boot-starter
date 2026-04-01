package com.polo.boot.web.spi;

public interface OperatorContextProvider {
    OperatorContext getCurrentOperator();

    static OperatorContextProvider anonymous() {
        return OperatorContext::anonymous;
    }
}
