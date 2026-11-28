import api from './api'
import { ordenesInicialesMock, mecanicosMock, repuestosDisponiblesMock } from '../data/ordenesMock'

const clonar = (data) => JSON.parse(JSON.stringify(data))

export const ESTADOS_ORDEN_VALIDOS = [
  'DIAGNOSTICO',
  'PENDIENTE',
  'EN_PROCESO',
  'FINALIZADA',
  'ENTREGADA',
  'CANCELADA'
]

export const ESTADOS_ORDEN_BLOQUEADOS_PIEZAS = [
  'FINALIZADA',
  'ENTREGADA',
  'CANCELADA'
]

let ordenesDB = clonar(ordenesInicialesMock)
let catalogoDB = clonar(repuestosDisponiblesMock)

const tieneSesionApi = () => Boolean(localStorage.getItem('sm_token'))
const esIdNumerico = (id) => typeof id === 'number' || /^\d+$/.test(String(id || ''))

export const calcularTotalOrden = (orden) => {
  if (!orden) return 0

  const estadoGarantia = orden.garantia?.estadoGarantia || 'PENDIENTE'
  const ordenCubiertaCompleta =
    Boolean(orden.ordenPadreGarantiaId) && estadoGarantia === 'RECLAMADA'

  const manoObra = ordenCubiertaCompleta
    ? 0
    : Math.max(0, Number(orden.costoManoObra) || 0)

  const piezasFacturables = (orden.solicitudesPiezas || []).reduce((acumulado, item) => {
    const estaAprobada = item.estado === 'APROBADA'
    const exentaGarantia = Boolean(item.cubiertoPorGarantia) || ordenCubiertaCompleta
    if (!estaAprobada || exentaGarantia) return acumulado

    const precioUnitario = Number(
      item.precioUnitarioHistorico ?? item.precioVenta ?? item.precio ?? 0
    )
    const cantidad = Math.max(0, Number(item.cantidad) || 0)
    return acumulado + (Math.max(0, precioUnitario) * cantidad)
  }, 0)

  return manoObra + piezasFacturables
}

const mapearDetalleDto = (dto) => {
  const repuestoLocal = catalogoDB.find((r) => String(r.id) === String(dto.piezaId))
  return {
    id: dto.id,
    piezaId: dto.piezaId,
    sku: dto.piezaSku || repuestoLocal?.sku || `REP-${dto.piezaId}`,
    repuesto: dto.piezaNombre || repuestoLocal?.nombre || `Pieza #${dto.piezaId}`,
    cantidad: Number(dto.cantidad) || 1,
    precioUnitarioHistorico: Number(dto.precioUnitarioHistorico ?? repuestoLocal?.precioVenta ?? 0),
    subtotal: Number(dto.subtotal ?? 0),
    cubiertoPorGarantia: Boolean(dto.cubiertoPorGarantia),
    estado: dto.estadoSolicitud || dto.estado || 'SOLICITADA',
    aprobadoPorId: dto.aprobadoPorId ?? null,
    aprobadoPorNombre: dto.aprobadoPorNombre ?? null,
    stockActual: repuestoLocal?.stock ?? 0
  }
}

const normalizarOrden = (orden) => {
  const copia = clonar(orden)
  copia.costoManoObra = Math.max(0, Number(copia.costoManoObra) || 0)
  copia.solicitudesPiezas = Array.isArray(copia.solicitudesPiezas) ? copia.solicitudesPiezas : []
  copia.garantia = copia.garantia || {
    estadoGarantia: 'PENDIENTE',
    checklist: {
      torque: false,
      frenos: false,
      sistemaElectrico: false,
      pruebaRuta: false,
      estanqueidadEmpaques: false
    },
    observaciones: '',
    dias: 30,
    kilometros: 1000,
    kmEntrega: null,
    seriesLotesPiezas: '',
    certificada: false
  }
  if (!copia.garantia.estadoGarantia) {
    copia.garantia.estadoGarantia = copia.garantia.certificada ? 'VIGENTE' : 'PENDIENTE'
  }
  copia.totalOrden = calcularTotalOrden(copia)
  return copia
}

ordenesDB = ordenesDB.map(normalizarOrden)

export const ordenesService = {
  async obtenerOrdenes() {
    return ordenesDB.map(normalizarOrden)
  },

  async obtenerMecanicos() {
    return clonar(mecanicosMock)
  },

  async obtenerRepuestos() {
    try {
      const { data } = await api.get('/piezas', { params: { size: 50 } })
      const contenido = Array.isArray(data) ? data : data?.content
      if (Array.isArray(contenido) && contenido.length > 0) {
        catalogoDB = contenido.map((p) => ({
          id: p.id,
          sku: p.sku || `REP-${p.id}`,
          nombre: p.nombre,
          stock: Number(p.stockActual ?? p.stock ?? 0),
          precioVenta: Number(p.precioVenta ?? p.precio ?? 0)
        }))
      }
    } catch {
      // Mantiene el catálogo local si el endpoint aún no está disponible o sin sesión activa
    }
    return clonar(catalogoDB)
  },

  async obtenerDetallesOrden(ordenId) {
    const orden = ordenesDB.find((o) => String(o.id) === String(ordenId))
    if (tieneSesionApi() && esIdNumerico(ordenId)) {
      try {
        const [{ data: detallesData }, { data: costosData }] = await Promise.all([
          api.get(`/ordenes/${ordenId}/detalles`),
          api.get(`/ordenes/${ordenId}/costos`)
        ])
        const mapeados = Array.isArray(detallesData) ? detallesData.map(mapearDetalleDto) : []
        if (orden) {
          orden.solicitudesPiezas = mapeados
          if (costosData?.costoManoObra != null) {
            orden.costoManoObra = Number(costosData.costoManoObra) || 0
          }
          orden.totalOrden =
            costosData?.totalOrden != null
              ? Number(costosData.totalOrden)
              : calcularTotalOrden(orden)
        }
        return {
          solicitudesPiezas: clonar(mapeados),
          totalOrden: orden ? orden.totalOrden : Number(costosData?.totalOrden ?? 0)
        }
      } catch {
        // Respaldo local si el backend no responde
      }
    }
    return {
      solicitudesPiezas: clonar(orden?.solicitudesPiezas || []),
      totalOrden: orden ? calcularTotalOrden(orden) : 0
    }
  },

  async crearOrden(payload) {
    const nuevaOrden = normalizarOrden({
      id: payload.id ?? `ORD-${String(ordenesDB.length + 1).padStart(3, '0')}`,
      cliente: payload.cliente,
      vehiculo: payload.vehiculo,
      placa: payload.placa,
      servicio: payload.servicio,
      mecanicoId: payload.mecanicoId ?? null,
      ordenPadreGarantiaId: payload.ordenPadreGarantiaId ?? null,
      estado: ESTADOS_ORDEN_VALIDOS.includes(payload.estado) ? payload.estado : 'PENDIENTE',
      motivoCancelacion: null,
      fechaIngreso: payload.fechaIngreso ?? new Date().toISOString().slice(0, 10),
      costoManoObra: payload.costoManoObra ?? 0,
      garantia: payload.garantia ?? {
        estadoGarantia: 'PENDIENTE',
        checklist: {
          torque: false,
          frenos: false,
          sistemaElectrico: false,
          pruebaRuta: false,
          estanqueidadEmpaques: false
        },
        observaciones: '',
        dias: 30,
        kilometros: 1000,
        kmEntrega: null,
        seriesLotesPiezas: '',
        certificada: false
      },
      solicitudesPiezas: payload.solicitudesPiezas ?? []
    })

    ordenesDB.unshift(nuevaOrden)
    return normalizarOrden(nuevaOrden)
  },

  async actualizarGarantiaOrden(ordenId, garantiaPayload, marcarFinalizada = false) {
    const orden = ordenesDB.find((o) => String(o.id) === String(ordenId))
    const nuevoEstado = marcarFinalizada ? 'FINALIZADA' : null
    const garantiaClonada = clonar(garantiaPayload)

    if (!garantiaClonada.estadoGarantia) {
      garantiaClonada.estadoGarantia = garantiaClonada.certificada ? 'VIGENTE' : 'PENDIENTE'
    }

    if (orden) {
      orden.garantia = garantiaClonada
      if (nuevoEstado && ESTADOS_ORDEN_VALIDOS.includes(nuevoEstado)) {
        orden.estado = nuevoEstado
      }
      orden.totalOrden = calcularTotalOrden(orden)
    }

    return {
      ordenId,
      garantia: garantiaClonada,
      nuevoEstado,
      totalOrden: orden ? orden.totalOrden : 0
    }
  },

  async solicitarPiezaOrden(ordenId, { piezaId, cantidad, nota = '', cubiertoPorGarantia = false }) {
    const orden = ordenesDB.find((o) => String(o.id) === String(ordenId))
    if (!orden) {
      throw new Error(`No se encontró la orden ${ordenId}.`)
    }

    if (ESTADOS_ORDEN_BLOQUEADOS_PIEZAS.includes(orden.estado)) {
      throw new Error(
        `La orden está en estado ${orden.estado}; no se admiten nuevas solicitudes de piezas.`
      )
    }

    const cant = Number(cantidad)
    if (!Number.isInteger(cant) || cant < 1) {
      throw new Error('La cantidad debe ser un número entero mayor a 0.')
    }

    const repuesto = catalogoDB.find((r) => String(r.id) === String(piezaId))
    if (!repuesto) {
      throw new Error('Selecciona un repuesto válido del inventario.')
    }

    if (tieneSesionApi() && esIdNumerico(ordenId)) {
      try {
        await api.post(`/ordenes/${ordenId}/detalles`, {
          piezaId: Number(repuesto.id),
          cantidad: cant,
          cubiertoPorGarantia: Boolean(cubiertoPorGarantia)
        })
        await this.obtenerRepuestos()
        const sincronizado = await this.obtenerDetallesOrden(ordenId)
        return {
          solicitudesPiezas: sincronizado.solicitudesPiezas,
          repuestos: clonar(catalogoDB),
          totalOrden: sincronizado.totalOrden
        }
      } catch (err) {
        if (err?.response?.data?.message) {
          throw new Error(err.response.data.message)
        }
      }
    }

    const nuevaSolicitud = {
      id: `SOL-${Date.now()}`,
      piezaId: repuesto.id,
      sku: repuesto.sku,
      repuesto: repuesto.nombre,
      cantidad: cant,
      precioUnitarioHistorico: Number(repuesto.precioVenta) || 0,
      nota: (nota || '').trim(),
      cubiertoPorGarantia: Boolean(cubiertoPorGarantia),
      estado: 'SOLICITADA',
      stockActual: repuesto.stock
    }

    orden.solicitudesPiezas = orden.solicitudesPiezas || []
    orden.solicitudesPiezas.push(nuevaSolicitud)
    orden.totalOrden = calcularTotalOrden(orden)

    return {
      solicitudesPiezas: clonar(orden.solicitudesPiezas),
      repuestos: clonar(catalogoDB),
      totalOrden: orden.totalOrden
    }
  },

  async gestionarSolicitudPieza(ordenId, solicitudId, { estado, cubiertoPorGarantia }) {
    const orden = ordenesDB.find((o) => String(o.id) === String(ordenId))
    if (!orden) {
      throw new Error(`No se encontró la orden ${ordenId}.`)
    }

    if (ESTADOS_ORDEN_BLOQUEADOS_PIEZAS.includes(orden.estado)) {
      throw new Error(
        `La orden está en estado ${orden.estado}; no se pueden aprobar ni rechazar piezas.`
      )
    }

    const solicitud = (orden.solicitudesPiezas || []).find((s) => String(s.id) === String(solicitudId))
    if (!solicitud) {
      throw new Error('No se encontró la solicitud de pieza indicada.')
    }

    if (solicitud.estado === 'REVERTIDA') {
      throw new Error('Una solicitud revertida no puede modificarse.')
    }

    if (estado === 'RECHAZADA' && solicitud.estado === 'APROBADA') {
      throw new Error('No se puede rechazar una pieza que ya fue aprobada.')
    }

    if (tieneSesionApi() && esIdNumerico(ordenId) && esIdNumerico(solicitudId)) {
      try {
        if (estado === 'APROBADA') {
          await api.patch(`/ordenes/${ordenId}/detalles/${solicitudId}/aprobar`)
        } else if (estado === 'RECHAZADA') {
          await api.patch(`/ordenes/${ordenId}/detalles/${solicitudId}/rechazar`)
        }
        await this.obtenerRepuestos()
        const sincronizado = await this.obtenerDetallesOrden(ordenId)
        return {
          solicitudesPiezas: sincronizado.solicitudesPiezas,
          repuestos: clonar(catalogoDB),
          totalOrden: sincronizado.totalOrden
        }
      } catch (err) {
        if (err?.response?.data?.message) {
          throw new Error(err.response.data.message)
        }
      }
    }

    const repuesto = catalogoDB.find((r) => String(r.id) === String(solicitud.piezaId))
    const cantidad = Number(solicitud.cantidad) || 0

    if (typeof cubiertoPorGarantia === 'boolean') {
      solicitud.cubiertoPorGarantia = cubiertoPorGarantia
    }

    if (estado === 'APROBADA' && solicitud.estado !== 'APROBADA') {
      if (repuesto) {
        if (repuesto.stock < cantidad) {
          throw new Error(
            `Stock insuficiente para "${repuesto.nombre}". Disponible: ${repuesto.stock} u., solicitado: ${cantidad} u.`
          )
        }
        repuesto.stock -= cantidad
      }
      solicitud.precioUnitarioHistorico = Number(
        solicitud.precioUnitarioHistorico ?? repuesto?.precioVenta ?? 0
      )
      solicitud.estado = 'APROBADA'
    } else if (estado === 'RECHAZADA' && solicitud.estado !== 'RECHAZADA') {
      solicitud.estado = 'RECHAZADA'
    }

    solicitud.stockActual = repuesto ? repuesto.stock : (solicitud.stockActual ?? 0)
    orden.totalOrden = calcularTotalOrden(orden)

    return {
      solicitudesPiezas: clonar(orden.solicitudesPiezas),
      repuestos: clonar(catalogoDB),
      totalOrden: orden.totalOrden
    }
  },

  async actualizarManoObraOrden(ordenId, costoManoObra) {
    const orden = ordenesDB.find((o) => String(o.id) === String(ordenId))
    if (!orden) {
      throw new Error(`No se encontró la orden ${ordenId}.`)
    }

    if (orden.estado === 'ENTREGADA' || orden.estado === 'CANCELADA') {
      throw new Error(`La orden está en estado ${orden.estado}; no se puede modificar la mano de obra.`)
    }

    const valorManoObra = Number(costoManoObra)
    if (Number.isNaN(valorManoObra) || valorManoObra < 0) {
      throw new Error('El costo de mano de obra no puede ser negativo.')
    }

    if (tieneSesionApi() && esIdNumerico(ordenId)) {
      try {
        const { data } = await api.patch(`/ordenes/${ordenId}/mano-obra`, {
          costoManoObra: valorManoObra
        })
        orden.costoManoObra = Number(data?.costoManoObra ?? valorManoObra)
        orden.totalOrden =
          data?.totalOrden != null ? Number(data.totalOrden) : calcularTotalOrden(orden)
        return {
          ordenId,
          costoManoObra: orden.costoManoObra,
          totalOrden: orden.totalOrden
        }
      } catch (err) {
        if (err?.response?.data?.message) {
          throw new Error(err.response.data.message)
        }
      }
    }

    orden.costoManoObra = valorManoObra
    orden.totalOrden = calcularTotalOrden(orden)

    return {
      ordenId,
      costoManoObra: orden.costoManoObra,
      totalOrden: orden.totalOrden
    }
  }
}