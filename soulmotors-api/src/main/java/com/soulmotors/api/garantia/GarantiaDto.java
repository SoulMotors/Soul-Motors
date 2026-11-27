package com.soulmotors.api.garantia;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GarantiaDto(
        Long id,
        Long ordenId,
        Integer kmEntrega,
        Integer diasCobertura,
        Integer kmLimiteCobertura,
        LocalDate fechaVencimiento,
        Boolean chkTorqueSeguridad,
        Boolean chkFrenosTransmision,
        Boolean chkSistemaElectrico,
        Boolean chkEstanqueidadEmpaques,
        Boolean chkPruebaRutaAprobada,
        String seriesLotesPiezas,
        String observacionesTecnicas,
        EstadoGarantia estadoGarantia,
        LocalDateTime creadoAt,
        LocalDateTime actualizadoAt
) {
    public static GarantiaDto from(GarantiaOrden g) {
        return new GarantiaDto(
                g.getId(),
                g.getOrden() != null ? g.getOrden().getId() : null,
                g.getKmEntrega(),
                g.getDiasCobertura(),
                g.getKmLimiteCobertura(),
                g.getFechaVencimiento(),
                g.getChkTorqueSeguridad(),
                g.getChkFrenosTransmision(),
                g.getChkSistemaElectrico(),
                g.getChkEstanqueidadEmpaques(),
                g.getChkPruebaRutaAprobada(),
                g.getSeriesLotesPiezas(),
                g.getObservacionesTecnicas(),
                g.getEstadoGarantia(),
                g.getCreadoAt(),
                g.getActualizadoAt()
        );
    }
}