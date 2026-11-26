package com.soulmotors.api.orden;


import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AvanzarEstadoRequest(
        Boolean viabilidadTecnica,

        @PositiveOrZero(message = "El costo de mano de obra no puede ser negativo")
        BigDecimal costoManoObra
) {} 
