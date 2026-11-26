package com.soulmotors.api.orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenTrabajoDto(
        Long id,
        Long vehiculoId,
        String placa,
        String vehiculoDescripcion,
        Long mecanicoAsignadoId,
        String mecanicoNombre,
        Long vendedorId,
        Long ordenPadreGarantiaId,
        EstadoOrden estado,
        String descripcionIngreso,
        Boolean viabilidadTecnica,
        BigDecimal costoManoObra,
        BigDecimal totalOrden,
        String motivoCancelacion,
        LocalDateTime creadoAt,
        LocalDateTime actualizadoAt
) {
    public static OrdenTrabajoDto from(OrdenTrabajo o) {
        String placa = o.getVehiculo() != null ? o.getVehiculo().getPlaca() : null;
        String descMoto = o.getVehiculo() != null
                ? o.getVehiculo().getMarca() + " " + o.getVehiculo().getModelo()
                : null;
        Long mecId = o.getMecanicoAsignado() != null ? o.getMecanicoAsignado().getId() : null;
        String mecNombre = o.getMecanicoAsignado() != null ? o.getMecanicoAsignado().getNombre() : null;

        return new OrdenTrabajoDto(
                o.getId(),
                o.getVehiculo() != null ? o.getVehiculo().getId() : null,
                placa,
                descMoto,
                mecId,
                mecNombre,
                o.getVendedor() != null ? o.getVendedor().getId() : null,
                o.getOrdenPadreGarantia() != null ? o.getOrdenPadreGarantia().getId() : null,
                o.getEstado(),
                o.getDescripcionIngreso(),
                o.getViabilidadTecnica(),
                o.getCostoManoObra(),
                o.getTotalOrden(),
                o.getMotivoCancelacion(),
                o.getCreadoAt(),
                o.getActualizadoAt()
        );
    }
}
