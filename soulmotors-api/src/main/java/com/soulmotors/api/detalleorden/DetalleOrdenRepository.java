package com.soulmotors.api.detalleorden;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Long> {

    List<DetalleOrden> findByOrdenIdOrderByCreadoAtAsc(Long ordenId);

    Optional<DetalleOrden> findByIdAndOrdenId(Long id, Long ordenId);

    List<DetalleOrden> findByOrdenIdAndEstadoSolicitud(Long ordenId, EstadoSolicitud estadoSolicitud);

    @Query(value = "SELECT fn_aprobar_pieza_orden(:detalleId, :adminId)", nativeQuery = true)
    void ejecutarAprobacionAtomica(@Param("detalleId") Long detalleId, @Param("adminId") Long adminId);
}
