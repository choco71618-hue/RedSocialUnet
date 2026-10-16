package com.redsocial.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveTutorSubjectRequest(
        @NotBlank(message = "El nombre de la materia es obligatorio")
        @Size(max = 120, message = "El nombre de la materia no puede superar los 120 caracteres")
        String subjectName,
        @NotNull(message = "El precio por hora es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio por hora debe ser mayor a cero")
        BigDecimal pricePerHour
) {
}
