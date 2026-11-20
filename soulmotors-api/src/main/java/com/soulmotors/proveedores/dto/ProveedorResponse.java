package com.soulmotors.proveedores.dto;

public record ProveedorResponse(
        Long id,
        String nit,
        String nombre,
        String telefono,
        String email,
        String direccion,
        Integer tiempoEntregaDias,
        Boolean activo
) {}
