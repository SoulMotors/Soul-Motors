package com.soulmotors.api.detalleorden;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordenes/{ordenId}")
public class DetalleOrdenController {

    private final DetalleOrdenService detalleService;

    public DetalleOrdenController(DetalleOrdenService detalleService) {
        this.detalleService = detalleService;
    }

    @GetMapping("/detalles")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR', 'MECANICO', 'GERENTE')")
    public List<DetalleOrdenDto> listarDetalles(@PathVariable Long ordenId) {
        return detalleService.listarPorOrden(ordenId);
    }

    @GetMapping("/costos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR', 'MECANICO', 'GERENTE')")
    public ResumenCostosOrdenDto obtenerCostos(@PathVariable Long ordenId) {
        return detalleService.obtenerResumenCostos(ordenId);
    }

    @PostMapping("/detalles")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'MECANICO')")
    public ResponseEntity<DetalleOrdenDto> solicitarPieza(
            @PathVariable Long ordenId,
            @Valid @RequestBody SolicitarPiezaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(detalleService.solicitarPieza(ordenId, request));
    }

    @PatchMapping("/detalles/{detalleId}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DetalleOrdenDto aprobarPieza(
            @PathVariable Long ordenId,
            @PathVariable Long detalleId,
            Authentication authentication) {
        return detalleService.aprobarPieza(ordenId, detalleId, authentication.getName());
    }

    @PatchMapping("/detalles/{detalleId}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DetalleOrdenDto rechazarPieza(
            @PathVariable Long ordenId,
            @PathVariable Long detalleId) {
        return detalleService.rechazarPieza(ordenId, detalleId);
    }

    @PatchMapping("/mano-obra")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResumenCostosOrdenDto actualizarManoObra(
            @PathVariable Long ordenId,
            @Valid @RequestBody ActualizarManoObraRequest request) {
        return detalleService.actualizarManoObra(ordenId, request);
    }
}