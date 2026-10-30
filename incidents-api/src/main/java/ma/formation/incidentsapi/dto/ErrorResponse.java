package ma.formation.incidentsapi.dto;

import java.util.List;

public record ErrorResponse(int status, String message, List<FieldErrorResponse> errors) {
    public ErrorResponse(int status, String message) {
        this(status, message, List.of());
    }
}
