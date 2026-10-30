package ma.formation.incidentsapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank(message = "Le nom est obligatoire") String fullName,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "L'email est invalide") String email
) {
}
