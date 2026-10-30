package ma.formation.incidentsapi.dto;

import jakarta.validation.constraints.NotNull;
import ma.formation.incidentsapi.model.IncidentStatus;

public record StatusUpdateRequest(@NotNull(message = "Le statut est obligatoire") IncidentStatus status) {
}
