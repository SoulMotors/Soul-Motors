package com.soulmotors.api.proveedores.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soulmotors.common.exception.ConflictException;
import com.soulmotors.common.exception.NotFoundException;
import com.soulmotors.api.proveedores.dto.ProveedorRequest;
import com.soulmotors.api.proveedores.dto.ProveedorResponse;
import com.soulmotors.api.proveedores.model.Proveedor;
import com.soulmotors.api.proveedores.repository.ProveedorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository repo;

    @Transactional
    public ProveedorResponse crear(ProveedorRequest r) {
        String nit = r.nit().trim();
        if (repo.existsByNitIgnoreCase(nit)) {
            throw new ConflictException("Ya existe un proveedor con el NIT " + nit);
        }
        Proveedor p = new Proveedor();
        aplicar(p, r);
        p.setActivo(true);
        return toResponse(repo.save(p));
    }

    @Transactional(readOnly = true)
    public Page<ProveedorResponse> listar(String q, Pageable pageable) {
        String filtro = q == null ? "" : q.trim();
        return repo.buscarActivos(filtro, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest r) {
        Proveedor p = buscar(id);
        String nit = r.nit().trim();
        if (repo.existsByNitIgnoreCaseAndIdNot(nit, id)) {
            throw new ConflictException("Ya existe otro proveedor con el NIT " + nit);
        }
        aplicar(p, r);
        return toResponse(repo.save(p));
    }

    // Baja lógica: los pedidos y vínculos históricos siguen apuntando a este proveedor
    @Transactional
    public void desactivar(Long id) {
        Proveedor p = buscar(id);
        p.setActivo(false);
        repo.save(p);
    }

    private Proveedor buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Proveedor " + id + " no existe"));
    }

    private void aplicar(Proveedor p, ProveedorRequest r) {
        p.setNit(r.nit().trim());
        p.setNombre(r.nombre().trim());
        p.setTelefono(limpiar(r.telefono()));
        p.setEmail(limpiar(r.email()));
        p.setDireccion(limpiar(r.direccion()));
        p.setTiempoEntregaDias(r.tiempoEntregaDias());
    }

    private String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ProveedorResponse toResponse(Proveedor p) {
        return new ProveedorResponse(p.getId(), p.getNit(), p.getNombre(), p.getTelefono(),
                p.getEmail(), p.getDireccion(), p.getTiempoEntregaDias(), p.getActivo());
    }
}
