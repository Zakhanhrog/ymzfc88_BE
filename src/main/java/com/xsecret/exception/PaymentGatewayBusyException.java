package com.xsecret.exception;

/**
 * Exception cho lỗi gateway bận (có thể retry)
 */
public class PaymentGatewayBusyException extends Exception {
    
    public PaymentGatewayBusyException(String message) {
        super(message);
    }
    
    public PaymentGatewayBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}

