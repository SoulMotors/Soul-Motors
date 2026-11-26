package com.soulmotors.api.orden;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    List<OrdenTrabajo> findByMecanicoAsignadoIdOrderByCreadoAtDesc(Long mecanicoId);

    List<OrdenTrabajo> findByEstadoOrderByCreadoAtDesc(EstadoOrden estado);

    List<OrdenTrabajo> findAllByOrderByCreadoAtDesc();

    @Query(value = """
            SELECT COUNT(*) FROM detalle_orden d
            JOIN pieza p ON p.id = d.pieza_id
            WHERE d.orden_id = :ordenId
              AND d.estado_solicitud = 'SOLICITADA'
              AND p.stock_actual < d.cantidad
            """, nativeQuery = true)
    long contarPiezasSinStockSuficiente(@Param("ordenId") Long ordenId);
}
