package com.soulmotors.api.proveedores.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.soulmotors.api.proveedores.dto.VinculoRequest;
import com.soulmotors.api.proveedores.dto.VinculoResponse;
import com.soulmotors.api.proveedores.dto.VinculoUpdateRequest;
import com.soulmotors.api.proveedores.service.PiezaProveedorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/piezas/{piezaId}/proveedores")
@RequiredArgsConstructor
public class PiezaProveedorController {

    private final PiezaProveedorService service;

    @PostMapping
    public ResponseEntity<VinculoResponse> vincular(@PathVariable Long piezaId, @Valid @RequestBody VinculoRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.vincular(piezaId, r));
    }

    @GetMapping
    public List<VinculoResponse> listar(@PathVariable Long piezaId) {
        return service.listar(piezaId);
    }

    @PutMapping("/{vinculoId}")
    public VinculoResponse actualizar(@PathVariable Long piezaId, @PathVariable Long vinculoId,
                                      @Valid @RequestBody VinculoUpdateRequest r) {
        return service.actualizar(piezaId, vinculoId, r);
    }

    @DeleteMapping("/{vinculoId}")
    public ResponseEntity<Void> desvincular(@PathVariable Long piezaId, @PathVariable Long vinculoId) {
        service.desvincular(piezaId, vinculoId);
        return ResponseEntity.noContent().build();
    }
}
