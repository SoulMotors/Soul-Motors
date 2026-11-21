package com.soulmotors.api.inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pieza")
@Getter
@Setter
public class Pieza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoPieza categoria;

    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

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
