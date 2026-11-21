package com.soulmotors.api.inventario.dto;

import com.soulmotors.api.inventario.model.TipoPieza;
import java.math.BigDecimal;

public record PiezaResponse(
        Long id,
        String sku,
        String nombre,
        TipoPieza categoria,
        BigDecimal precioVenta,
        Integer stockActual,
        Integer stockMinimo,
        boolean bajoMinimo
) {}
