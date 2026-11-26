package com.soulmotors.api.orden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarOrdenRequest(
        @NotBlank(message = "El motivo de cancelación es obligatorio")
        @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
        String motivoCancelacion
) {}