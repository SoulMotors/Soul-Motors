// ResumenCostosOrdenDto.java
package com.soulmotors.api.detalleorden;

import java.math.BigDecimal;

public record ResumenCostosOrdenDto(
        Long ordenId,
        BigDecimal costoManoObra,
        BigDecimal subtotalPiezasCobrables,
        BigDecimal subtotalPiezasGarantia,
        BigDecimal totalOrden
) {}