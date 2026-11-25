package com.soulmotors.api.orden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CrearOrdenRequest(
        @NotNull(message = "El vehiculoId es obligatorio")
        Long vehiculoId,

        @NotNull(message = "El mecanicoAsignadoId es obligatorio")
        Long mecanicoAsignadoId,

        Long vendedorId,

        Long ordenPadreGarantiaId,

        @NotBlank(message = "La descripción de ingreso es obligatoria")
        String descripcionIngreso,

        @PositiveOrZero(message = "El costo de mano de obra no puede ser negativo")
        BigDecimal costoManoObra
) {}