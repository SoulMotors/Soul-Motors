package com.soulmotors.api.garantia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record EmitirGarantiaRequest(
        @NotNull(message = "El kmEntrega es obligatorio")
        @PositiveOrZero(message = "El kilometraje de entrega no puede ser negativo")
        Integer kmEntrega,

        @Positive(message = "Los días de cobertura deben ser mayores a 0")
        Integer diasCobertura,

        @NotNull(message = "El kmLimiteCobertura es obligatorio")
        @PositiveOrZero
        Integer kmLimiteCobertura,

        @NotNull Boolean chkTorqueSeguridad,
        @NotNull Boolean chkFrenosTransmision,
        @NotNull Boolean chkSistemaElectrico,
        @NotNull Boolean chkEstanqueidadEmpaques,
        @NotNull Boolean chkPruebaRutaAprobada,

        String seriesLotesPiezas,
        String observacionesTecnicas
) {}