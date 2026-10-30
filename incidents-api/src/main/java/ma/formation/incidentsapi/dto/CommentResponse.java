package ma.formation.incidentsapi.dto;

import java.time.Instant;

public record CommentResponse(Long id, String content, Instant createdAt, UserSummary author) {
}
