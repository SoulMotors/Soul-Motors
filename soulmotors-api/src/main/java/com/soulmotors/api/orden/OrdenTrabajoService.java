package com.soulmotors.api.orden;

import com.soulmotors.api.common.ConflictoException;
import com.soulmotors.api.common.NoEncontradoException;
import com.soulmotors.api.garantia.GarantiaOrdenRepository;
import com.soulmotors.api.moto.Vehiculo;
import com.soulmotors.api.moto.VehiculoRepository;
import com.soulmotors.api.usuario.Rol;
import com.soulmotors.api.usuario.Usuario;
import com.soulmotors.api.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository ordenRepository;
    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GarantiaOrdenRepository garantiaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public OrdenTrabajoService(OrdenTrabajoRepository ordenRepository,
                               VehiculoRepository vehiculoRepository,
                               UsuarioRepository usuarioRepository,
                               GarantiaOrdenRepository garantiaRepository) {
        this.ordenRepository = ordenRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.garantiaRepository = garantiaRepository;
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoDto> listar(EstadoOrden estado) {
        Usuario actual = obtenerUsuarioAutenticado();
        List<OrdenTrabajo> ordenes;

        if (actual.getRol() == Rol.MECANICO) {
            ordenes = ordenRepository.findByMecanicoAsignadoIdOrderByCreadoAtDesc(actual.getId());
            if (estado != null) {
                ordenes = ordenes.stream().filter(o -> o.getEstado() == estado).toList();
            }
        } else if (estado != null) {
            ordenes = ordenRepository.findByEstadoOrderByCreadoAtDesc(estado);
        } else {
            ordenes = ordenRepository.findAllByOrderByCreadoAtDesc();
        }

        return ordenes.stream().map(OrdenTrabajoDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OrdenTrabajoDto obtenerPorId(Long id) {
        OrdenTrabajo orden = buscarOrden(id);
        Usuario actual = obtenerUsuarioAutenticado();

        if (actual.getRol() == Rol.MECANICO
                && (orden.getMecanicoAsignado() == null || !orden.getMecanicoAsignado().getId().equals(actual.getId()))) {
            throw new AccessDeniedException("No tiene permiso para consultar una orden asignada a otro mecánico.");
        }
        return OrdenTrabajoDto.from(orden);
    }

    @Transactional
    public OrdenTrabajoDto crear(CrearOrdenRequest req) {
        Usuario actual = obtenerUsuarioAutenticado();
        registrarActorEnSesionBd(actual);

        Vehiculo vehiculo = vehiculoRepository.findById(req.vehiculoId())
                .orElseThrow(() -> new NoEncontradoException("No existe el vehículo con id=" + req.vehiculoId()));

        Usuario mecanico = usuarioRepository.findById(req.mecanicoAsignadoId())
                .orElseThrow(() -> new NoEncontradoException("No existe el mecánico con id=" + req.mecanicoAsignadoId()));

        if (!mecanico.isActivo() || mecanico.getRol() != Rol.MECANICO) {
            throw new ConflictoException("El empleado asignado debe tener rol MECANICO y estar activo.");
        }

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setVehiculo(vehiculo);
        orden.setMecanicoAsignado(mecanico);
        orden.setDescripcionIngreso(req.descripcionIngreso().trim());

        if (req.costoManoObra() != null) {
            orden.setCostoManoObra(req.costoManoObra());
        }

        if (req.vendedorId() != null) {
            Usuario vendedor = usuarioRepository.findById(req.vendedorId())
                    .orElseThrow(() -> new NoEncontradoException("No existe el vendedor con id=" + req.vendedorId()));
            orden.setVendedor(vendedor);
        } else if (actual.getRol() == Rol.VENDEDOR) {
            orden.setVendedor(actual);
        }

        if (req.ordenPadreGarantiaId() != null) {
            OrdenTrabajo ordenPadre = buscarOrden(req.ordenPadreGarantiaId());
            if (!ordenPadre.getVehiculo().getId().equals(vehiculo.getId())) {
                throw new ConflictoException("La orden padre de garantía pertenece a otro vehículo.");
            }
            orden.setOrdenPadreGarantia(ordenPadre);
        }

        return OrdenTrabajoDto.from(ordenRepository.save(orden));
    }

    @Transactional
    public OrdenTrabajoDto avanzarEstado(Long id, AvanzarEstadoRequest req) {
        OrdenTrabajo orden = buscarOrden(id);
        Usuario actual = obtenerUsuarioAutenticado();
        registrarActorEnSesionBd(actual);

        if (orden.getEstado() == EstadoOrden.ENTREGADA || orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new ConflictoException("La orden está en estado " + orden.getEstado() + " y es de solo lectura.");
        }

        if (actual.getRol() == Rol.MECANICO
                && (orden.getMecanicoAsignado() == null || !orden.getMecanicoAsignado().getId().equals(actual.getId()))) {
            throw new AccessDeniedException("Solo el mecánico asignado puede avanzar esta orden.");
        }

        if (req != null) {
            if (req.viabilidadTecnica() != null) {
                orden.setViabilidadTecnica(req.viabilidadTecnica());
            }
            if (req.costoManoObra() != null) {
                if (actual.getRol() != Rol.ADMINISTRADOR) {
                    throw new AccessDeniedException("Solo un ADMINISTRADOR puede modificar el costo de mano de obra.");
                }
                orden.setCostoManoObra(req.costoManoObra());
            }
        }

        EstadoOrden siguiente = switch (orden.getEstado()) {
            case RECIBIDA -> EstadoOrden.DIAGNOSTICO;
            case DIAGNOSTICO -> {
                if (actual.getRol() == Rol.MECANICO) {
                    throw new AccessDeniedException("Solo un ADMINISTRADOR puede aprobar el presupuesto y pasar la orden a PENDIENTE.");
                }
                validarRequisitosParaPendiente(orden);
                yield EstadoOrden.PENDIENTE;
            }
            case PENDIENTE -> {
                long sinStock = ordenRepository.contarPiezasSinStockSuficiente(orden.getId());
                if (sinStock > 0) {
                    throw new ConflictoException("No hay stock suficiente para " + sinStock + " pieza(s) solicitada(s) en esta orden.");
                }
                yield EstadoOrden.EN_PROCESO;
            }
            case EN_PROCESO -> {
                if (!garantiaRepository.existsByOrdenId(orden.getId())) {
                    throw new ConflictoException("Debe registrar el Checklist Técnico de calidad antes de finalizar la orden.");
                }
                yield EstadoOrden.FINALIZADA;
            }
            case FINALIZADA -> {
                if (!garantiaRepository.existsByOrdenId(orden.getId())) {
                    throw new ConflictoException("Debe emitir el certificado de garantía antes de entregar la orden.");
                }
                yield EstadoOrden.ENTREGADA;
            }
            default -> throw new ConflictoException("Transición no permitida desde " + orden.getEstado());
        };

        orden.setEstado(siguiente);
        OrdenTrabajo guardada = ordenRepository.saveAndFlush(orden);
        entityManager.refresh(guardada);
        return OrdenTrabajoDto.from(guardada);
    }

    @Transactional
    public OrdenTrabajoDto retrocederEstado(Long id) {
        OrdenTrabajo orden = buscarOrden(id);
        Usuario actual = obtenerUsuarioAutenticado();
        registrarActorEnSesionBd(actual);

        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new ConflictoException("Una orden CANCELADA es inmutable y no puede retrocederse.");
        }
        if (orden.getEstado() == EstadoOrden.ENTREGADA) {
            throw new ConflictoException("Una orden ENTREGADA es un estado terminal inmutable y no puede retrocederse.");
        }

        EstadoOrden anterior = switch (orden.getEstado()) {
            case PENDIENTE -> EstadoOrden.DIAGNOSTICO;
            case EN_PROCESO -> EstadoOrden.PENDIENTE;
            case FINALIZADA -> EstadoOrden.EN_PROCESO;
            default -> throw new ConflictoException("No es posible retroceder desde el estado " + orden.getEstado());
        };

        orden.setEstado(anterior);
        OrdenTrabajo guardada = ordenRepository.saveAndFlush(orden);
        entityManager.refresh(guardada);
        return OrdenTrabajoDto.from(guardada);
    }

    @Transactional
    public OrdenTrabajoDto cancelar(Long id, CancelarOrdenRequest req) {
        OrdenTrabajo orden = buscarOrden(id);
        Usuario actual = obtenerUsuarioAutenticado();
        registrarActorEnSesionBd(actual);

        if (orden.getEstado() == EstadoOrden.ENTREGADA || orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new ConflictoException("No se puede cancelar una orden en estado " + orden.getEstado());
        }

        orden.setMotivoCancelacion(req.motivoCancelacion().trim());
        orden.setEstado(EstadoOrden.CANCELADA);

        OrdenTrabajo guardada = ordenRepository.saveAndFlush(orden);
        entityManager.refresh(guardada);
        return OrdenTrabajoDto.from(guardada);
    }

    private void validarRequisitosParaPendiente(OrdenTrabajo orden) {
        if (!Boolean.TRUE.equals(orden.getViabilidadTecnica())) {
            throw new ConflictoException("La orden requiere viabilidad técnica aprobada (true) para pasar a PENDIENTE.");
        }
        if (orden.getCostoManoObra() == null || orden.getCostoManoObra().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictoException("La orden requiere un costo de mano de obra mayor a 0 para pasar a PENDIENTE.");
        }
        Usuario mec = orden.getMecanicoAsignado();
        if (mec == null || !mec.isActivo() || mec.getRol() != Rol.MECANICO) {
            throw new ConflictoException("La orden requiere un mecánico activo asignado.");
        }
    }

    private OrdenTrabajo buscarOrden(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la orden de trabajo con id=" + id));
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NoEncontradoException("Usuario autenticado no encontrado: " + auth.getName()));
    }

    private void registrarActorEnSesionBd(Usuario usuario) {
        entityManager.createNativeQuery("SELECT set_config('app.current_user_id', :uid, true)")
                .setParameter("uid", String.valueOf(usuario.getId()))
                .getSingleResult();
    }
}