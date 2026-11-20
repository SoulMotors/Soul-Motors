package com.soulmotors.api.proveedores.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.soulmotors.inventario.model.Pieza;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pieza_proveedor")
@Getter @Setter @NoArgsConstructor
public class PiezaProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pieza_id", nullable = false)
    private Pieza pieza;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    /** 1 = Principal; 2..N = Alternativos (orden de reasignación en el Sprint 3). */
    @Column(nullable = false)
    private Integer prioridad;

    @Column(name = "costo_pactado", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPactado;

    @Column(name = "creado_at", updatable = false)
    private LocalDateTime creadoAt;

    @Column(name = "actualizado_at")
    private LocalDateTime actualizadoAt;

    @PrePersist
    protected void onCreate() {
        creadoAt = LocalDateTime.now();
        actualizadoAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoAt = LocalDateTime.now();
    }
}
