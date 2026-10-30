package ma.formation.incidentsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.formation.incidentsapi.model.IncidentCategory;
import ma.formation.incidentsapi.model.IncidentPriority;

public record IncidentRequest(
        @NotBlank(message = "Le titre est obligatoire") @Size(max = 200, message = "Le titre est trop long") String title,
        @NotBlank(message = "La description est obligatoire") @Size(max = 4000, message = "La description est trop longue") String description,
        @NotNull(message = "La catégorie est obligatoire") IncidentCategory category,
        @NotNull(message = "La priorité est obligatoire") IncidentPriority priority
) {
}
