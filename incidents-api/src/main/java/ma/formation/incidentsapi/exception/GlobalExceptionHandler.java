package ma.formation.incidentsapi.exception;

import ma.formation.incidentsapi.dto.ErrorResponse;
import ma.formation.incidentsapi.dto.FieldErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleApi(ApiException exception) {
        return response(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return org.springframework.http.ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Données invalides", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return response(400, "Corps de requête invalide");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException exception) {
        return response(409, "Cette opération entre en conflit avec une donnée existante");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException exception) {
        return response(403, "Accès interdit");
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return response(500, "Une erreur interne est survenue");
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
    }

    private org.springframework.http.ResponseEntity<ErrorResponse> response(int status, String message) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.valueOf(status))
                .body(new ErrorResponse(status, message));
    }
}
