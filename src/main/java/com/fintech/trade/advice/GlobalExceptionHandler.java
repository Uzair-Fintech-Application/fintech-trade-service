package com.fintech.trade.advice;

import com.fintech.trade.exception.MarketDeviationException;
import com.fintech.trade.exception.ResourceNotFoundException;
import com.fintech.trade.exception.TradeStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(TradeStateException.class)
    public ResponseEntity<Map<String, Object>> handleTradeState(TradeStateException ex) {
        log.warn("EXCEPTION | type=TradeState | message={}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("EXCEPTION | type=NotFound | message={}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MarketDeviationException.class)
    public ResponseEntity<Map<String, Object>> handleMarketDeviation(MarketDeviationException ex) {
        log.warn("EXCEPTION | type=MarketDeviation | message={}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ResourceAccessException ex) {
        log.error("EXCEPTION | type=ServiceUnavailable | message={}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, msg("error.service.unavailable"));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleUpstreamError(HttpClientErrorException ex) {
        log.warn("EXCEPTION | type=UpstreamError | status={} | message={}", ex.getStatusCode(), ex.getMessage());
        String message;
        if (ex.getStatusCode().value() == 404) {
            message = msg("error.upstream.wallet.not.found");
        } else if (ex.getStatusCode().value() == 409) {
            message = msg("error.upstream.insufficient.funds");
        } else {
            message = msg("error.upstream.generic");
        }
        return buildResponse(HttpStatus.valueOf(ex.getStatusCode().value()), message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("EXCEPTION | type=AccessDenied | message={}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, msg("error.access.denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        log.warn("EXCEPTION | type=Authentication | class={} | message={}", ex.getClass().getSimpleName(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, msg("error.authentication.required"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("EXCEPTION | type=IllegalArgument | message={}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors()
                .forEach(e -> errors.put(((FieldError) e).getField(), e.getDefaultMessage()));
        log.warn("EXCEPTION | type=Validation | errors={}", errors);
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", msg("error.validation.error"),
                "message", msg("error.validation.failed"),
                "details", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("EXCEPTION | type=MessageNotReadable | message={}", ex.getMessage());
        String message = msg("error.request.body.invalid");
        if (ex.getMessage() != null && ex.getMessage().contains("Currency")) {
            message = msg("error.currency.invalid");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("EXCEPTION | type=MissingHeader | header={}", ex.getHeaderName());
        String message = msg("error.header.missing", ex.getHeaderName());
        if ("Authorization".equalsIgnoreCase(ex.getHeaderName())) {
            message = msg("error.header.authorization.missing");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("EXCEPTION | type=MissingParam | param={}", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST, msg("error.param.missing", ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("EXCEPTION | type=TypeMismatch | param={} | value={}", ex.getName(), ex.getValue());
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return buildResponse(HttpStatus.BAD_REQUEST, msg("error.type.mismatch", ex.getValue(), ex.getName(), expectedType));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("EXCEPTION | type=MethodNotSupported | method={}", ex.getMethod());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, msg("error.method.not.supported", ex.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("EXCEPTION | type=MediaTypeNotSupported | contentType={}", ex.getContentType());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, msg("error.media.type.not.supported", ex.getContentType()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("EXCEPTION | type=NoResourceFound | path={}", ex.getResourcePath());
        return buildResponse(HttpStatus.NOT_FOUND, msg("error.endpoint.not.found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("EXCEPTION | type=Unhandled | class={} | message={}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg("error.internal"));
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, Locale.getDefault());
    }
}
