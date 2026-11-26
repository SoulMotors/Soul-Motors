package com.soulmotors.api.orden;

import com.soulmotors.api.moto.Vehiculo;
import com.soulmotors.api.usuario.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden_trabajo")
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mecanico_asignado_id", nullable = false)
    private Usuario mecanicoAsignado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_padre_garantia_id")
    private OrdenTrabajo ordenPadreGarantia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.DIAGNOSTICO;

    @Column(name = "descripcion_ingreso", nullable = false, columnDefinition = "TEXT")
    private String descripcionIngreso;

    @Column(name = "viabilidad_tecnica", nullable = false)
    private Boolean viabilidadTecnica = Boolean.TRUE;

    @Column(name = "costo_mano_obra", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoManoObra = BigDecimal.ZERO;

    @Column(name = "total_orden", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOrden = BigDecimal.ZERO;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

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
    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }
    public Usuario getMecanicoAsignado() { return mecanicoAsignado; }
    public void setMecanicoAsignado(Usuario mecanicoAsignado) { this.mecanicoAsignado = mecanicoAsignado; }
    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }
    public OrdenTrabajo getOrdenPadreGarantia() { return ordenPadreGarantia; }
    public void setOrdenPadreGarantia(OrdenTrabajo ordenPadreGarantia) { this.ordenPadreGarantia = ordenPadreGarantia; }
    public EstadoOrden getEstado() { return estado; }
    public void setEstado(EstadoOrden estado) { this.estado = estado; }
    public String getDescripcionIngreso() { return descripcionIngreso; }
    public void setDescripcionIngreso(String descripcionIngreso) { this.descripcionIngreso = descripcionIngreso; }
    public Boolean getViabilidadTecnica() { return viabilidadTecnica; }
    public void setViabilidadTecnica(Boolean viabilidadTecnica) { this.viabilidadTecnica = viabilidadTecnica; }
    public BigDecimal getCostoManoObra() { return costoManoObra; }
    public void setCostoManoObra(BigDecimal costoManoObra) { this.costoManoObra = costoManoObra; }
    public BigDecimal getTotalOrden() { return totalOrden; }
    public void setTotalOrden(BigDecimal totalOrden) { this.totalOrden = totalOrden; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }
    public LocalDateTime getCreadoAt() { return creadoAt; }
    public LocalDateTime getActualizadoAt() { return actualizadoAt; }
}