package com.cooperativa.fintech.domain.common.exception;

public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
