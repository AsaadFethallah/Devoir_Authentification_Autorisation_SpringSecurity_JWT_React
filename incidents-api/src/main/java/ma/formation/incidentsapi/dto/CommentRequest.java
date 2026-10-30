package ma.formation.incidentsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Le contenu est obligatoire") @Size(max = 2000, message = "La note est trop longue") String content
) {
}
