package ma.formation.incidentsapi.dto;

public record ProfileResponse(Long id, String fullName, String email, String role, boolean enabled) {
}
