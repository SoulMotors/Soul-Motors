package com.soulmotors.api.garantia;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ordenes/{ordenId}/garantia")
public class GarantiaOrdenController {

    private final GarantiaOrdenService garantiaService;

    public GarantiaOrdenController(GarantiaOrdenService garantiaService) {
        this.garantiaService = garantiaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR', 'MECANICO', 'GERENTE')")
    public GarantiaDto obtener(@PathVariable Long ordenId) {
        return garantiaService.obtenerPorOrden(ordenId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'MECANICO')")
    public ResponseEntity<GarantiaDto> registrar(@PathVariable Long ordenId,
                                                 @Valid @RequestBody EmitirGarantiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(garantiaService.registrarChecklistYGarantia(ordenId, request));
    }

    @PatchMapping("/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public GarantiaDto cambiarEstado(@PathVariable Long ordenId,
                                     @RequestParam EstadoGarantia estado) {
        return garantiaService.actualizarEstado(ordenId, estado);
    }
}