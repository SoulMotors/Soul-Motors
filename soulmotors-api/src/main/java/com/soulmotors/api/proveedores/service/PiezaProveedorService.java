package com.soulmotors.api.proveedores.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soulmotors.common.exception.BusinessException;
import com.soulmotors.common.exception.ConflictException;
import com.soulmotors.common.exception.NotFoundException;
import com.soulmotors.inventario.model.Pieza;
import com.soulmotors.inventario.repository.PiezaRepository;
import com.soulmotors.api.proveedores.dto.VinculoRequest;
import com.soulmotors.api.proveedores.dto.VinculoResponse;
import com.soulmotors.api.proveedores.dto.VinculoUpdateRequest;
import com.soulmotors.api.proveedores.model.PiezaProveedor;
import com.soulmotors.api.proveedores.model.Proveedor;
import com.soulmotors.api.proveedores.repository.PiezaProveedorRepository;
import com.soulmotors.api.proveedores.repository.ProveedorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PiezaProveedorService {

    private final PiezaProveedorRepository repo;
    private final ProveedorRepository proveedorRepo;
    private final PiezaRepository piezaRepo;

    @Transactional
    public VinculoResponse vincular(Long piezaId, VinculoRequest r) {
        Pieza pieza = bloquearPieza(piezaId);
        Proveedor prov = proveedorRepo.findById(r.proveedorId())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new NotFoundException("Proveedor activo " + r.proveedorId() + " no existe"));

        if (repo.existsByPieza_IdAndProveedor_Id(piezaId, prov.getId())) {
            throw new ConflictException("Ese proveedor ya está vinculado a la pieza");
        }
        long maxPermitida = repo.countByPieza_Id(piezaId) + 1;
        if (r.prioridad() > maxPermitida) {
            throw new BusinessException("La prioridad debe ser consecutiva: el máximo permitido es " + maxPermitida);
        }
        if (repo.existsByPieza_IdAndPrioridad(piezaId, r.prioridad())) {
            throw new ConflictException("La prioridad " + r.prioridad() + " ya está ocupada para esta pieza; "
                    + "edite el vínculo existente para intercambiar prioridades");
        }
        PiezaProveedor v = new PiezaProveedor();
        v.setPieza(pieza);
        v.setProveedor(prov);
        v.setPrioridad(r.prioridad());
        v.setCostoPactado(r.costoPactado());
        return toResponse(repo.save(v));
    }

    @Transactional(readOnly = true)
    public List<VinculoResponse> listar(Long piezaId) {
        if (!piezaRepo.existsById(piezaId)) {
            throw new NotFoundException("Pieza " + piezaId + " no existe");
        }
        return repo.findByPieza_IdOrderByPrioridadAsc(piezaId).stream().map(this::toResponse).toList();
    }

    /**
     * Si la prioridad pedida la tiene otro proveedor de la misma pieza, se intercambian.
     * Es posible porque uq_pieza_prioridad es DEFERRABLE INITIALLY DEFERRED (se valida al COMMIT).
     */
    @Transactional
    public VinculoResponse actualizar(Long piezaId, Long vinculoId, VinculoUpdateRequest r) {
        bloquearPieza(piezaId);
        PiezaProveedor v = buscar(piezaId, vinculoId);
        Integer actual = v.getPrioridad();
        Integer nueva = r.prioridad();

        if (!actual.equals(nueva)) {
            long total = repo.countByPieza_Id(piezaId);
            if (nueva > total) {
                throw new BusinessException("La prioridad debe estar entre 1 y " + total
                        + " (las prioridades son consecutivas)");
            }
            repo.findByPieza_IdAndPrioridad(piezaId, nueva).ifPresent(otro -> otro.setPrioridad(actual));
            v.setPrioridad(nueva);
        }
        v.setCostoPactado(r.costoPactado());
        return toResponse(repo.save(v));
    }

    /** Desvincula y reordena las prioridades para que queden consecutivas (1, 2, 3...). */
    @Transactional
    public void desvincular(Long piezaId, Long vinculoId) {
        bloquearPieza(piezaId);
        repo.delete(buscar(piezaId, vinculoId));
        repo.flush();
        List<PiezaProveedor> restantes = repo.findByPieza_IdOrderByPrioridadAsc(piezaId);
        for (int i = 0; i < restantes.size(); i++) {
            PiezaProveedor v = restantes.get(i);
            if (v.getPrioridad() != i + 1) {
                v.setPrioridad(i + 1);
            }
        }
    }

    // Mismo bloqueo que usa PiezaService.registrarEntrada: serializa los cambios de prioridades por pieza.
    private Pieza bloquearPieza(Long piezaId) {
        return piezaRepo.findByIdForUpdate(piezaId)
                .orElseThrow(() -> new NotFoundException("Pieza " + piezaId + " no existe"));
    }

    private PiezaProveedor buscar(Long piezaId, Long vinculoId) {
        return repo.findByIdAndPieza_Id(vinculoId, piezaId)
                .orElseThrow(() -> new NotFoundException("Vínculo " + vinculoId + " no existe para la pieza " + piezaId));
    }

    private VinculoResponse toResponse(PiezaProveedor v) {
        Proveedor p = v.getProveedor();
        return new VinculoResponse(v.getId(), v.getPieza().getId(), p.getId(), p.getNit(), p.getNombre(),
                p.getActivo(), p.getTiempoEntregaDias(), v.getPrioridad(),
                v.getPrioridad() == 1 ? "PRINCIPAL" : "ALTERNATIVO", v.getCostoPactado());
    }
}
