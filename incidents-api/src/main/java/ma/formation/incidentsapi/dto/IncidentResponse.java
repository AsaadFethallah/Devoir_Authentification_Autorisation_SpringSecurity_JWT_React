package ma.formation.incidentsapi.dto;

import ma.formation.incidentsapi.model.IncidentCategory;
import ma.formation.incidentsapi.model.IncidentPriority;
import ma.formation.incidentsapi.model.IncidentStatus;

import java.time.Instant;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        IncidentCategory category,
        IncidentPriority priority,
        IncidentStatus status,
        Instant createdAt,
        Instant updatedAt,
        UserSummary createdBy,
        UserSummary assignedTo
) {
}
