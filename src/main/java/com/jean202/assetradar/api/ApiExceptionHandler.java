package com.jean202.assetradar.api;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            ServerWebExchange exchange
    ) {
        return badRequest("INVALID_REQUEST", exception.getMessage(), exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiErrorResponse> handleServerWebInput(
            ServerWebInputException exception,
            ServerWebExchange exchange
    ) {
        return badRequest("INVALID_REQUEST", messageOf(exception), exchange);
    }

    private ResponseEntity<ApiErrorResponse> badRequest(
            String code,
            String message,
            ServerWebExchange exchange
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        code,
                        message,
                        exchange.getRequest().getPath().value()
                ));
    }

    private String messageOf(ServerWebInputException exception) {
        MethodParameter parameter = exception.getMethodParameter();
        if (parameter == null) {
            return "request parameters are invalid";
        }

        String parameterName = parameter.getParameterName();
        if ("baseAmount".equals(parameterName) || BigDecimal.class.equals(parameter.getParameterType())) {
            return "baseAmount must be a valid decimal number";
        }

        if (parameterName == null || parameterName.isBlank()) {
            return "request parameters are invalid";
        }

        return parameterName + " is invalid";
    }
}
