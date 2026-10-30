package ma.formation.incidentsapi.dto;

import java.time.Instant;

public record HistoryResponse(Long id, Instant date, String action, UserSummary author) {
}
