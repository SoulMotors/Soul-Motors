// ActualizarManoObraRequest.java
package com.soulmotors.api.detalleorden;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ActualizarManoObraRequest(
        @NotNull(message = "El costo de mano de obra es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo de mano de obra no puede ser negativo")
        BigDecimal costoManoObra
) {}
