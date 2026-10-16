package com.redsocial.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateProjectRequest(
        @NotNull(message = "El tipo de proyecto es obligatorio")
        ProjectKind kind,
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 160, message = "El título no puede superar los 160 caracteres")
        String title,
        @Size(max = 4000, message = "La descripción no puede superar los 4000 caracteres")
        String description,
        LocalDateTime eventDate,
        @Size(max = 200, message = "La ubicación no puede superar los 200 caracteres")
        String location,
        @DecimalMin(value = "0.0", message = "El monto objetivo no puede ser negativo")
        BigDecimal targetAmount,
        ProjectStatus status
) {
}
