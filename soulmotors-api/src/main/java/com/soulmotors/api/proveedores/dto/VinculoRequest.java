package com.soulmotors.api.proveedores.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VinculoRequest(
        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        @NotNull(message = "La prioridad es obligatoria (1 = Principal)")
        @Min(value = 1, message = "La prioridad debe ser mayor o igual a 1")
        Integer prioridad,

        @NotNull(message = "El costo pactado es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo pactado debe ser mayor o igual a 0")
        @Digits(integer = 8, fraction = 2, message = "El costo pactado admite hasta 8 enteros y 2 decimales")
        BigDecimal costoPactado
) {}
