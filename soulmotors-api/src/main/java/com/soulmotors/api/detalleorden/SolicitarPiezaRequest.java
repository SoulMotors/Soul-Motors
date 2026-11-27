// SolicitarPiezaRequest.java
package com.soulmotors.api.detalleorden;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SolicitarPiezaRequest(
        @NotNull(message = "El ID de la pieza es obligatorio")
        Long piezaId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a 0")
        Integer cantidad,

        Boolean cubiertoPorGarantia
) {}