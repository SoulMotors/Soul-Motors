package com.soulmotors.api.proveedores.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.soulmotors.api.proveedores.model.PiezaProveedor;

public interface PiezaProveedorRepository extends JpaRepository<PiezaProveedor, Long> {

    @EntityGraph(attributePaths = "proveedor")
    List<PiezaProveedor> findByPieza_IdOrderByPrioridadAsc(Long piezaId);

    @EntityGraph(attributePaths = "proveedor")
    Optional<PiezaProveedor> findByIdAndPieza_Id(Long id, Long piezaId);

    Optional<PiezaProveedor> findByPieza_IdAndPrioridad(Long piezaId, Integer prioridad);

    boolean existsByPieza_IdAndProveedor_Id(Long piezaId, Long proveedorId);

    boolean existsByPieza_IdAndPrioridad(Long piezaId, Integer prioridad);

    long countByPieza_Id(Long piezaId);
}
