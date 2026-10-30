package ma.formation.incidentsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "L'ancien mot de passe est obligatoire") String currentPassword,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire") @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères") String newPassword,
        @NotBlank(message = "La confirmation est obligatoire") String confirmation
) {
}
