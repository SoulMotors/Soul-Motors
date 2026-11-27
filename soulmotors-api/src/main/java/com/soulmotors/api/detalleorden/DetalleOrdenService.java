package com.soulmotors.api.detalleorden;

import com.soulmotors.api.common.ConflictoException;
import com.soulmotors.api.common.NoEncontradoException;
import com.soulmotors.api.inventario.model.Pieza;
import com.soulmotors.api.inventario.repository.PiezaRepository;
import com.soulmotors.api.orden.EstadoOrden;
import com.soulmotors.api.orden.OrdenTrabajo;
import com.soulmotors.api.orden.OrdenTrabajoRepository;
import com.soulmotors.api.usuario.Usuario;
import com.soulmotors.api.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DetalleOrdenService {

    private final DetalleOrdenRepository detalleRepository;
    private final OrdenTrabajoRepository ordenRepository;
    private final PiezaRepository piezaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    public DetalleOrdenService(DetalleOrdenRepository detalleRepository,
                               OrdenTrabajoRepository ordenRepository,
                               PiezaRepository piezaRepository,
                               UsuarioRepository usuarioRepository,
                               EntityManager entityManager) {
        this.detalleRepository = detalleRepository;
        this.ordenRepository = ordenRepository;
        this.piezaRepository = piezaRepository;
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<DetalleOrdenDto> listarPorOrden(Long ordenId) {
        OrdenTrabajo orden = buscarOrden(ordenId);
        validarTitularidadMecanico(orden);
        return detalleRepository.findByOrdenIdOrderByCreadoAtAsc(ordenId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DetalleOrdenDto solicitarPieza(Long ordenId, SolicitarPiezaRequest request) {
        OrdenTrabajo orden = buscarOrden(ordenId);
        validarTitularidadMecanico(orden);

        if (orden.getEstado() == EstadoOrden.FINALIZADA
                || orden.getEstado() == EstadoOrden.ENTREGADA
                || orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new ConflictoException(
                    "No se pueden solicitar piezas para una orden en estado " + orden.getEstado());
        }

        boolean cubiertoPorGarantia = Boolean.TRUE.equals(request.cubiertoPorGarantia());
        if (cubiertoPorGarantia && !esAdministrador() && orden.getOrdenPadreGarantia() == null) {
            throw new ConflictoException(
                    "Solo un administrador o una orden de reingreso por garantía puede marcar piezas cubiertas por garantía.");
        }

        Pieza pieza = piezaRepository.findById(request.piezaId())
                .orElseThrow(() -> new NoEncontradoException("Pieza no encontrada"));

        DetalleOrden detalle = new DetalleOrden();
        detalle.setOrden(orden);
        detalle.setPieza(pieza);
        detalle.setCantidad(request.cantidad());
        detalle.setPrecioUnitarioHistorico(pieza.getPrecioVenta());
        detalle.setCubiertoPorGarantia(cubiertoPorGarantia);
        detalle.setEstadoSolicitud(EstadoSolicitud.SOLICITADA);

        return toDto(detalleRepository.save(detalle));
    }

    @Transactional
    public DetalleOrdenDto aprobarPieza(Long ordenId, Long detalleId, String usernameAdmin) {
        Usuario admin = usuarioRepository.findByUsername(usernameAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        DetalleOrden detalle = detalleRepository.findByIdAndOrdenId(detalleId, ordenId)
                .orElseThrow(() -> new NoEncontradoException("Detalle de orden no encontrado"));

        // Invoca la función SQL atómica de Leider (valida rol, stock, descuenta, congela precio y audita)
        try {
            detalleRepository.ejecutarAprobacionAtomica(detalle.getId(), admin.getId());
        } catch (DataAccessException ex) {
            Throwable causa = ex.getMostSpecificCause();
            String mensaje = (causa != null && causa.getMessage() != null)
                    ? causa.getMessage().split("\n")[0]
                    : "Error al aprobar la pieza en la orden.";
            throw new ConflictoException(mensaje);
        }

        // Limpia caché de Hibernate para leer el estado actualizado por la función SQL
        entityManager.flush();
        entityManager.clear();

        recalcularTotalOrden(ordenId);

        DetalleOrden actualizado = detalleRepository.findById(detalleId)
                .orElseThrow(() -> new NoEncontradoException("Detalle no encontrado tras aprobar"));
        return toDto(actualizado);
    }

    @Transactional
    public DetalleOrdenDto rechazarPieza(Long ordenId, Long detalleId) {
        DetalleOrden detalle = detalleRepository.findByIdAndOrdenId(detalleId, ordenId)
                .orElseThrow(() -> new NoEncontradoException("Detalle de orden no encontrado"));

        if (detalle.getEstadoSolicitud() != EstadoSolicitud.SOLICITADA) {
            throw new ConflictoException("Solo se pueden rechazar solicitudes en estado SOLICITADA");
        }

        detalle.setEstadoSolicitud(EstadoSolicitud.RECHAZADA);
        return toDto(detalleRepository.save(detalle));
    }

    @Transactional
    public ResumenCostosOrdenDto actualizarManoObra(Long ordenId, ActualizarManoObraRequest request) {
        OrdenTrabajo orden = buscarOrden(ordenId);
        if (orden.getEstado() == EstadoOrden.ENTREGADA || orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new ConflictoException(
                    "No se puede modificar la mano de obra de una orden " + orden.getEstado());
        }

        orden.setCostoManoObra(request.costoManoObra());
        ordenRepository.save(orden);

        return recalcularTotalOrden(ordenId);
    }

    @Transactional(readOnly = true)
    public ResumenCostosOrdenDto obtenerResumenCostos(Long ordenId) {
        OrdenTrabajo orden = buscarOrden(ordenId);
        validarTitularidadMecanico(orden);
        return calcularResumen(orden);
    }

    @Transactional
    public ResumenCostosOrdenDto recalcularTotalOrden(Long ordenId) {
        OrdenTrabajo orden = buscarOrden(ordenId);
        ResumenCostosOrdenDto resumen = calcularResumen(orden);
        orden.setTotalOrden(resumen.totalOrden());
        ordenRepository.save(orden);
        return resumen;
    }

    private void validarTitularidadMecanico(OrdenTrabajo orden) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;

        boolean esMecanico = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MECANICO"));

        if (esMecanico) {
            String usernameAutenticado = auth.getName();
            if (orden.getMecanicoAsignado() == null
                      || !usernameAutenticado.equalsIgnoreCase(orden.getMecanicoAsignado().getUsername())) {
               throw new AccessDeniedException(
                        "No tienes permiso para acceder a una orden que no está asignada a ti.");
    }
}
    }

    private boolean esAdministrador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    private ResumenCostosOrdenDto calcularResumen(OrdenTrabajo orden) {
        List<DetalleOrden> aprobados = detalleRepository
                .findByOrdenIdAndEstadoSolicitud(orden.getId(), EstadoSolicitud.APROBADA);

        BigDecimal subtotalCobrable = BigDecimal.ZERO;
        BigDecimal subtotalGarantia = BigDecimal.ZERO;

        for (DetalleOrden d : aprobados) {
            BigDecimal precio = d.getPrecioUnitarioHistorico() != null
                    ? d.getPrecioUnitarioHistorico()
                    : BigDecimal.ZERO;
            BigDecimal linea = precio.multiply(BigDecimal.valueOf(d.getCantidad()));

            if (Boolean.TRUE.equals(d.getCubiertoPorGarantia())) {
                subtotalGarantia = subtotalGarantia.add(linea);
            } else {
                subtotalCobrable = subtotalCobrable.add(linea);
            }
        }

        BigDecimal manoObra = orden.getCostoManoObra() != null ? orden.getCostoManoObra() : BigDecimal.ZERO;
        BigDecimal total = manoObra.add(subtotalCobrable);

        return new ResumenCostosOrdenDto(
                orden.getId(),
                manoObra,
                subtotalCobrable,
                subtotalGarantia,
                total
        );
    }

    private OrdenTrabajo buscarOrden(Long ordenId) {
        return ordenRepository.findById(ordenId)
                .orElseThrow(() -> new NoEncontradoException("Orden de trabajo no encontrada"));
    }

    private DetalleOrdenDto toDto(DetalleOrden d) {
        BigDecimal precio = d.getPrecioUnitarioHistorico() != null
                ? d.getPrecioUnitarioHistorico()
                : BigDecimal.ZERO;
        BigDecimal subtotal = Boolean.TRUE.equals(d.getCubiertoPorGarantia())
                ? BigDecimal.ZERO
                : precio.multiply(BigDecimal.valueOf(d.getCantidad()));

        return new DetalleOrdenDto(
                d.getId(),
                d.getOrden().getId(),
                d.getPieza().getId(),
                d.getPieza().getSku(),
                d.getPieza().getNombre(),
                d.getCantidad(),
                d.getPrecioUnitarioHistorico(),
                subtotal,
                d.getCubiertoPorGarantia(),
                d.getEstadoSolicitud(),
                d.getAprobadoPor() != null ? d.getAprobadoPor().getId() : null,
                d.getAprobadoPor() != null ? d.getAprobadoPor().getNombre() : null,
                d.getCreadoAt()
        );
    }
}