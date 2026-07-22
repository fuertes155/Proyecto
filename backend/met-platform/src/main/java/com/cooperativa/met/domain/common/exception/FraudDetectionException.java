package com.cooperativa.met.domain.common.exception;

public class FraudDetectionException extends BusinessRuleException {

    public FraudDetectionException(String code, String message) {
        super(code, message);
    }
}
