package ma.formation.incidentsapi.dto;

import java.time.Instant;

public record AdminUserResponse(Long id, String fullName, String email, String role, boolean enabled, Instant createdAt) {
}
