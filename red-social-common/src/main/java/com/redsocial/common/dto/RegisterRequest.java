package com.redsocial.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$",
                message = "El usuario debe tener entre 3 y 30 caracteres (letras, números o guion bajo)")
        String username,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@unet\\.edu\\.ve$",
                message = "Usá tu correo institucional @unet.edu.ve")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
        String password,
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String displayName,
        UserRole role,
        @Size(max = 120, message = "La carrera no puede superar los 120 caracteres")
        String career,
        @Size(max = 120, message = "La materia de interés no puede superar los 120 caracteres")
        String subjectOfInterest,
        TutorType tutorType,
        @Pattern(regexp = "^$|^\\d{6,10}$",
                message = "El carnet debe tener entre 6 y 10 dígitos")
        String carnet
) {
    /** Compatibilidad con el registro social clásico (sin rol académico). */
    public RegisterRequest(String username, String email, String password, String displayName) {
        this(username, email, password, displayName, null, null, null, null, null);
    }
}
