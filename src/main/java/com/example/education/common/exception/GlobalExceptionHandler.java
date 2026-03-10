package com.example.education.common.exception;

import com.example.education.common.api.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        if (ex.getCode() >= 500) {
            log.error("Business exception", ex);
        } else {
            log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        }
        return ApiResponse.fail(ex.getCode(), defaultIfBlank(ex.getMessage(), "业务处理失败"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return ApiResponse.fail(422, resolveFieldErrorMessage(ex.getBindingResult().getFieldErrors(), "参数校验失败"));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ApiResponse<Void> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<String> messages = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> defaultIfBlank(error.getDefaultMessage(), error.toString()))
                .filter(Objects::nonNull)
                .filter(message -> !message.isBlank())
                .distinct()
                .toList();
        return ApiResponse.fail(422, messages.isEmpty() ? "参数校验失败" : String.join("; ", messages));
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        return ApiResponse.fail(422, resolveFieldErrorMessage(ex.getBindingResult().getFieldErrors(), "参数绑定失败"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .filter(item -> !item.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));
        return ApiResponse.fail(422, defaultIfBlank(message, "参数校验失败"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return ApiResponse.fail(400, "缺少请求参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ApiResponse<Void> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        return ApiResponse.fail(400, "缺少请求头: " + ex.getHeaderName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ApiResponse.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ApiResponse<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ApiResponse.fail(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "请求内容类型不支持");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ApiResponse.fail(413, "文件过大");
    }

    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> handleMultipartException(MultipartException ex) {
        return ApiResponse.fail(400, "文件上传请求无效");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ApiResponse.fail(404, "请求路径不存在");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.fail(500, "系统错误");
    }

    private String resolveFieldErrorMessage(List<FieldError> fieldErrors, String defaultMessage) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return defaultMessage;
        }
        FieldError fieldError = fieldErrors.get(0);
        return defaultIfBlank(fieldError.getDefaultMessage(), fieldError.getField() + "不合法");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
