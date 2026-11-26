package com.soulmotors.api.garantia;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GarantiaOrdenRepository extends JpaRepository<GarantiaOrden, Long> {
    Optional<GarantiaOrden> findByOrdenId(Long ordenId);
    boolean existsByOrdenId(Long ordenId);
}