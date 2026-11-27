// DetalleOrdenDto.java
package com.soulmotors.api.detalleorden;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DetalleOrdenDto(
        Long id,
        Long ordenId,
        Long piezaId,
        String piezaSku,
        String piezaNombre,
        Integer cantidad,
        BigDecimal precioUnitarioHistorico,
        BigDecimal subtotal,
        Boolean cubiertoPorGarantia,
        EstadoSolicitud estadoSolicitud,
        Long aprobadoPorId,
        String aprobadoPorNombre,
        LocalDateTime creadoAt
) {}
