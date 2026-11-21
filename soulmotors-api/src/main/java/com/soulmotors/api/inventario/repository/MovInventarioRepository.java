package com.soulmotors.api.inventario.repository;

import com.soulmotors.api.inventario.model.MovInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovInventarioRepository extends JpaRepository<MovInventario, Long> {
    Page<MovInventario> findByPieza_IdOrderByIdDesc(Long piezaId, Pageable pageable);
}
