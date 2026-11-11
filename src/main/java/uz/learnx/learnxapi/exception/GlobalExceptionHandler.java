package uz.learnx.learnxapi.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import uz.learnx.learnxapi.model.ApiResponse;

/**
 * Every error leaves the API as ApiResponse JSON with a stable machine-readable {@code meta.code}
 * the UI can translate, and never leaks internals to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> api(ApiException e) {
        return error(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ApiResponse body = ApiResponse.error(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed");
        body.getMeta().put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    /** Invalid query parameters or path variables, such as a page size above the limit. */
    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse> invalidParameter(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiResponse> typeMismatch(TypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_parameter", "A request parameter has the wrong format");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> unreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "malformed_request", "Request body is missing or malformed");
    }

    /** Spring's own errors (404, 405, 415, missing parameter…) keep their status; anything else is our bug. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> unexpected(Exception e) {
        if (e instanceof ErrorResponse known) {
            HttpStatus status = HttpStatus.valueOf(known.getStatusCode().value());
            return error(status, status.name().toLowerCase(Locale.ROOT), status.getReasonPhrase());
        }
        log.error("Unhandled exception", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Something went wrong");
    }

    private static ResponseEntity<ApiResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(status, code, message));
    }
}
