package ma.formation.incidentsapi.dto;

public record AuthResponse(String token, String role, String fullName, String refreshToken) {
}
