package com.soulmotors.inventario.repository;

import com.soulmotors.inventario.model.Pieza;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PiezaRepository extends JpaRepository<Pieza, Long>, JpaSpecificationExecutor<Pieza> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pieza p WHERE p.id = :id")
    Optional<Pieza> findByIdForUpdate(@Param("id") Long id);
}
