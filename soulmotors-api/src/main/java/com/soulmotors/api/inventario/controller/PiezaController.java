package com.soulmotors.api.inventario.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.soulmotors.api.inventario.dto.EntradaStockRequest;
import com.soulmotors.api.inventario.dto.MovimientoResponse;
import com.soulmotors.api.inventario.dto.PiezaRequest;
import com.soulmotors.api.inventario.dto.PiezaResponse;
import com.soulmotors.api.inventario.model.TipoPieza;
import com.soulmotors.api.inventario.service.PiezaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/piezas")
@RequiredArgsConstructor
public class PiezaController {

    private final PiezaService service;

    @PostMapping
    public ResponseEntity<PiezaResponse> crear(@Valid @RequestBody PiezaRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(r));
    }

    @GetMapping
    public Page<PiezaResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TipoPieza tipo,
            @RequestParam(defaultValue = "false") boolean bajoMinimo,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return service.listar(q, tipo, bajoMinimo, pageable);
    }

    @GetMapping("/{id}")
    public PiezaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public PiezaResponse actualizar(@PathVariable Long id, @Valid @RequestBody PiezaRequest r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/entradas")
    public ResponseEntity<PiezaResponse> entrada(@PathVariable Long id, @Valid @RequestBody EntradaStockRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEntrada(id, r));
    }

    @GetMapping("/{id}/movimientos")
    public Page<MovimientoResponse> movimientos(@PathVariable Long id,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return service.movimientos(id, pageable);
    }
}
