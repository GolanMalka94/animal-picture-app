package com.example.animalpictures.web;

import com.example.animalpictures.client.ImageSourceException;
import com.example.animalpictures.domain.AnimalType;
import com.example.animalpictures.service.PictureNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/** Maps errors to RFC 9457 problem details ({@code application/problem+json}). */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(PictureNotFoundException.class)
    ProblemDetail handleNotFound(PictureNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Picture not found", e.getMessage());
    }

    @ExceptionHandler(ImageSourceException.class)
    ProblemDetail handleImageSource(ImageSourceException e) {
        log.warn("External picture source failed: {}", e.getMessage(), e.getCause());
        return problem(HttpStatus.BAD_GATEWAY, "Picture source unavailable", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String detail = e.getRequiredType() == AnimalType.class
                ? "Unknown animal type '" + e.getValue() + "'. Allowed values: " + AnimalType.allowedValues()
                : "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'";
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    /** Bean-validation failures on request parameters, e.g. {@code count=50}. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Invalid request", detail));
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
