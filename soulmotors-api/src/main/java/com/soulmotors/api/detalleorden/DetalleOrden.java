package com.soulmotors.api.detalleorden;

import com.soulmotors.api.inventario.model.Pieza;
import com.soulmotors.api.orden.OrdenTrabajo;
import com.soulmotors.api.usuario.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "detalle_orden")
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pieza_id", nullable = false)
    private Pieza pieza;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario_historico", precision = 12, scale = 2)
    private BigDecimal precioUnitarioHistorico;

    @Column(name = "cubierto_por_garantia", nullable = false)
    private Boolean cubiertoPorGarantia = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_solicitud", nullable = false, length = 20)
    private EstadoSolicitud estadoSolicitud = EstadoSolicitud.SOLICITADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por_id")
    private Usuario aprobadoPor;

    @Column(name = "creado_at", updatable = false)
    private LocalDateTime creadoAt;

    @Column(name = "actualizado_at")
    private LocalDateTime actualizadoAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.creadoAt = now;
        this.actualizadoAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.actualizadoAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public OrdenTrabajo getOrden() { return orden; }
    public void setOrden(OrdenTrabajo orden) { this.orden = orden; }
    public Pieza getPieza() { return pieza; }
    public void setPieza(Pieza pieza) { this.pieza = pieza; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitarioHistorico() { return precioUnitarioHistorico; }
    public void setPrecioUnitarioHistorico(BigDecimal precioUnitarioHistorico) { this.precioUnitarioHistorico = precioUnitarioHistorico; }
    public Boolean getCubiertoPorGarantia() { return cubiertoPorGarantia; }
    public void setCubiertoPorGarantia(Boolean cubiertoPorGarantia) { this.cubiertoPorGarantia = cubiertoPorGarantia; }
    public EstadoSolicitud getEstadoSolicitud() { return estadoSolicitud; }
    public void setEstadoSolicitud(EstadoSolicitud estadoSolicitud) { this.estadoSolicitud = estadoSolicitud; }
    public Usuario getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Usuario aprobadoPor) { this.aprobadoPor = aprobadoPor; }
    public LocalDateTime getCreadoAt() { return creadoAt; }
    public LocalDateTime getActualizadoAt() { return actualizadoAt; }
}
