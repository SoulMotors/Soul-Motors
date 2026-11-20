package com.soulmotors.inventario.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soulmotors.common.exception.BusinessException;
import com.soulmotors.common.exception.ConflictException;
import com.soulmotors.common.exception.NotFoundException;
import com.soulmotors.inventario.dto.EntradaStockRequest;
import com.soulmotors.inventario.dto.MovimientoResponse;
import com.soulmotors.inventario.dto.PiezaRequest;
import com.soulmotors.inventario.dto.PiezaResponse;
import com.soulmotors.inventario.model.MovInventario;
import com.soulmotors.inventario.model.Pieza;
import com.soulmotors.inventario.model.TipoMovimiento;
import com.soulmotors.inventario.model.TipoPieza;
import com.soulmotors.inventario.repository.MovInventarioRepository;
import com.soulmotors.inventario.repository.PiezaRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PiezaService {

    private final PiezaRepository piezaRepo;
    private final MovInventarioRepository movRepo;

    @Transactional
    public PiezaResponse crear(PiezaRequest r) {
        String skuLimpio = r.sku().trim();
        if (piezaRepo.existsBySkuIgnoreCase(skuLimpio)) {
            throw new ConflictException("Ya existe una pieza con el SKU " + skuLimpio);
        }
        int inicial = r.stockInicial() == null ? 0 : r.stockInicial();
        if (inicial < 0) {
            throw new BusinessException("El stock inicial no puede ser negativo");
        }

        Pieza p = new Pieza();
        p.setSku(skuLimpio);
        p.setNombre(r.nombre().trim());
        p.setCategoria(r.categoria());
        p.setPrecioVenta(r.precioVenta());
        p.setStockMinimo(r.stockMinimo());
        p.setStockActual(inicial);
        p = piezaRepo.save(p);

        if (inicial > 0) {
            guardarMovimiento(p, TipoMovimiento.Entrada, inicial);
        }
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public Page<PiezaResponse> listar(String q, TipoPieza tipo, boolean bajoMinimo, Pageable pageable) {
        Specification<Pieza> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), like),
                        cb.like(cb.lower(root.get("sku")), like)));
            }
            if (tipo != null) {
                ps.add(cb.equal(root.get("categoria"), tipo));
            }
            if (bajoMinimo) {
                ps.add(cb.lessThanOrEqualTo(root.get("stockActual"), root.get("stockMinimo")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return piezaRepo.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PiezaResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public PiezaResponse actualizar(Long id, PiezaRequest r) {
        Pieza p = buscar(id);
        String skuLimpio = r.sku().trim();
        if (piezaRepo.existsBySkuIgnoreCaseAndIdNot(skuLimpio, id)) {
            throw new ConflictException("Ya existe otra pieza con el SKU " + skuLimpio);
        }
        p.setSku(skuLimpio);
        p.setNombre(r.nombre().trim());
        p.setCategoria(r.categoria());
        p.setPrecioVenta(r.precioVenta());
        p.setStockMinimo(r.stockMinimo());
        return toResponse(piezaRepo.save(p));
    }

    @Transactional
    public void desactivar(Long id) {
        Pieza p = buscar(id);
        piezaRepo.delete(p);
    }

    @Transactional
    public PiezaResponse registrarEntrada(Long id, EntradaStockRequest r) {
        if (r.cantidad() == null || r.cantidad() <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a 0");
        }
        Pieza p = piezaRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Pieza " + id + " no existe"));

        int nuevo = p.getStockActual() + r.cantidad();
        p.setStockActual(nuevo);
        piezaRepo.save(p);

        guardarMovimiento(p, TipoMovimiento.Entrada, r.cantidad());
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoResponse> movimientos(Long piezaId, Pageable pageable) {
        buscar(piezaId);
        return movRepo.findByPieza_IdOrderByIdDesc(piezaId, pageable)
                .map(m -> new MovimientoResponse(
                        m.getId(),
                        m.getPieza().getId(),
                        m.getTipoMovimiento(),
                        m.getCantidad(),
                        m.getCreadoAt()));
    }

    private Pieza buscar(Long id) {
        return piezaRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Pieza " + id + " no existe"));
    }

    private void guardarMovimiento(Pieza pieza, TipoMovimiento tipo, int cantidad) {
        MovInventario m = new MovInventario();
        m.setPieza(pieza);
        m.setTipoMovimiento(tipo);
        m.setCantidad(cantidad);
        movRepo.save(m);
    }

    private PiezaResponse toResponse(Pieza p) {
        return new PiezaResponse(
                p.getId(),
                p.getSku(),
                p.getNombre(),
                p.getCategoria(),
                p.getPrecioVenta(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getStockActual() <= p.getStockMinimo());
    }
}
