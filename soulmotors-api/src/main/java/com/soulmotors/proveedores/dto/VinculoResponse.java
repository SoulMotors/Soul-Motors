package com.soulmotors.proveedores.dto;

import java.math.BigDecimal;

public record VinculoResponse(
        Long id,
        Long piezaId,
        Long proveedorId,
        String proveedorNit,
        String proveedorNombre,
        Boolean proveedorActivo,
        Integer tiempoEntregaDias,
        Integer prioridad,
        String rol,              // PRINCIPAL (prioridad 1) o ALTERNATIVO
        BigDecimal costoPactado
) {}
