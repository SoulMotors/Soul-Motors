package com.soulmotors.api.garantia;

import com.soulmotors.api.orden.OrdenTrabajo;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "garantia_orden")
public class GarantiaOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private OrdenTrabajo orden;

    @Column(name = "km_entrega", nullable = false)
    private Integer kmEntrega;

    @Column(name = "dias_cobertura", nullable = false)
    private Integer diasCobertura = 30;

    @Column(name = "km_limite_cobertura", nullable = false)
    private Integer kmLimiteCobertura;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "chk_torque_seguridad", nullable = false)
    private Boolean chkTorqueSeguridad = Boolean.FALSE;

    @Column(name = "chk_frenos_transmision", nullable = false)
    private Boolean chkFrenosTransmision = Boolean.FALSE;

    @Column(name = "chk_sistema_electrico", nullable = false)
    private Boolean chkSistemaElectrico = Boolean.FALSE;

    @Column(name = "chk_estanqueidad_empaques", nullable = false)
    private Boolean chkEstanqueidadEmpaques = Boolean.FALSE;

    @Column(name = "chk_prueba_ruta_aprobada", nullable = false)
    private Boolean chkPruebaRutaAprobada = Boolean.FALSE;

    @Column(name = "series_lotes_piezas", columnDefinition = "TEXT")
    private String seriesLotesPiezas;

    @Column(name = "observaciones_tecnicas", columnDefinition = "TEXT")
    private String observacionesTecnicas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_garantia", nullable = false, length = 30)
    private EstadoGarantia estadoGarantia = EstadoGarantia.VIGENTE;

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
    public Integer getKmEntrega() { return kmEntrega; }
    public void setKmEntrega(Integer kmEntrega) { this.kmEntrega = kmEntrega; }
    public Integer getDiasCobertura() { return diasCobertura; }
    public void setDiasCobertura(Integer diasCobertura) { this.diasCobertura = diasCobertura; }
    public Integer getKmLimiteCobertura() { return kmLimiteCobertura; }
    public void setKmLimiteCobertura(Integer kmLimiteCobertura) { this.kmLimiteCobertura = kmLimiteCobertura; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Boolean getChkTorqueSeguridad() { return chkTorqueSeguridad; }
    public void setChkTorqueSeguridad(Boolean chkTorqueSeguridad) { this.chkTorqueSeguridad = chkTorqueSeguridad; }
    public Boolean getChkFrenosTransmision() { return chkFrenosTransmision; }
    public void setChkFrenosTransmision(Boolean chkFrenosTransmision) { this.chkFrenosTransmision = chkFrenosTransmision; }
    public Boolean getChkSistemaElectrico() { return chkSistemaElectrico; }
    public void setChkSistemaElectrico(Boolean chkSistemaElectrico) { this.chkSistemaElectrico = chkSistemaElectrico; }
    public Boolean getChkEstanqueidadEmpaques() { return chkEstanqueidadEmpaques; }
    public void setChkEstanqueidadEmpaques(Boolean chkEstanqueidadEmpaques) { this.chkEstanqueidadEmpaques = chkEstanqueidadEmpaques; }
    public Boolean getChkPruebaRutaAprobada() { return chkPruebaRutaAprobada; }
    public void setChkPruebaRutaAprobada(Boolean chkPruebaRutaAprobada) { this.chkPruebaRutaAprobada = chkPruebaRutaAprobada; }
    public String getSeriesLotesPiezas() { return seriesLotesPiezas; }
    public void setSeriesLotesPiezas(String seriesLotesPiezas) { this.seriesLotesPiezas = seriesLotesPiezas; }
    public String getObservacionesTecnicas() { return observacionesTecnicas; }
    public void setObservacionesTecnicas(String observacionesTecnicas) { this.observacionesTecnicas = observacionesTecnicas; }
    public EstadoGarantia getEstadoGarantia() { return estadoGarantia; }
    public void setEstadoGarantia(EstadoGarantia estadoGarantia) { this.estadoGarantia = estadoGarantia; }
    public LocalDateTime getCreadoAt() { return creadoAt; }
    public LocalDateTime getActualizadoAt() { return actualizadoAt; }
}