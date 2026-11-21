package com.soulmotors.api.proveedores.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.soulmotors.api.proveedores.model.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    @Query("""
            SELECT p FROM Proveedor p
            WHERE p.activo = true
              AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.nit) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Proveedor> buscarActivos(@Param("q") String q, Pageable pageable);

    // El UNIQUE de proveedor.nit en BD cubre también a los inactivos, por eso no se filtra por activo.
    boolean existsByNitIgnoreCase(String nit);

    boolean existsByNitIgnoreCaseAndIdNot(String nit, Long id);
}
