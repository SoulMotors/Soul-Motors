package com.soulmotors.inventario.dto;

import com.soulmotors.inventario.model.TipoMovimiento;
import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        Long piezaId,
        TipoMovimiento tipoMovimiento,
        Integer cantidad,
        LocalDateTime creadoAt
) {}
