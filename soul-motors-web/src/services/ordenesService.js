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

let ordenesDB = clonar(ordenesInicialesMock)
let catalogoDB = clonar(repuestosDisponiblesMock)

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
    const orden = ordenesDB.find((o) => o.id === ordenId)
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

  async actualizarEstadoOrden(ordenId, nuevoEstado, motivo = null) {
    if (!ESTADOS_ORDEN_VALIDOS.includes(nuevoEstado)) {
      throw new Error(`Estado de orden no válido: ${nuevoEstado}`)
    }

    const orden = ordenesDB.find((o) => o.id === ordenId)
    const motivoCancelacion = nuevoEstado === 'CANCELADA' ? motivo : null

    if (orden) {
      if (nuevoEstado === 'CANCELADA' && orden.estado !== 'CANCELADA') {
        orden.solicitudesPiezas.forEach((item) => {
          if (item.estado === 'APROBADA') {
            const pieza = catalogoDB.find((p) => p.id === item.piezaId)
            if (pieza) {
              pieza.stock += Number(item.cantidad) || 0
            }
            item.estado = 'REVERTIDA'
          }
        })
      }
      orden.estado = nuevoEstado
      orden.motivoCancelacion = motivoCancelacion
      orden.totalOrden = calcularTotalOrden(orden)
    }

    return {
      ordenId,
      nuevoEstado,
      motivoCancelacion,
      solicitudesPiezas: orden ? clonar(orden.solicitudesPiezas) : [],
      totalOrden: orden ? orden.totalOrden : 0
    }
  },

  async solicitarPiezaOrden(ordenId, { piezaId, cantidad, nota, cubiertoPorGarantia = false }) {
    const orden = ordenesDB.find((o) => o.id === ordenId)
    if (!orden) {
      throw new Error(`No se encontró la orden ${ordenId}`)
    }
    if (orden.estado === 'ENTREGADA' || orden.estado === 'CANCELADA') {
      throw new Error(`La orden está en estado ${orden.estado} y no admite nuevas solicitudes.`)
    }

    const cant = Number(cantidad)
    if (!Number.isFinite(cant) || cant <= 0) {
      throw new Error('La cantidad solicitada debe ser mayor a 0.')
    }

    const pieza = catalogoDB.find((p) => p.id === piezaId)
    if (!pieza) {
      throw new Error('Selecciona un repuesto válido del inventario.')
    }

    const nuevaSolicitud = {
      id: Date.now() + Math.floor(Math.random() * 1000),
      piezaId: pieza.id,
      sku: pieza.sku,
      repuesto: pieza.nombre,
      cantidad: cant,
      precioUnitarioHistorico: Number(pieza.precioVenta) || 0,
      cubiertoPorGarantia: Boolean(cubiertoPorGarantia),
      stockActual: Number(pieza.stock) || 0,
      estado: 'SOLICITADA',
      nota: (nota || '').trim() || 'Solicitud desde taller'
    }

    orden.solicitudesPiezas.unshift(nuevaSolicitud)
    orden.totalOrden = calcularTotalOrden(orden)

    return {
      ordenId,
      solicitud: clonar(nuevaSolicitud),
      solicitudesPiezas: clonar(orden.solicitudesPiezas),
      repuestos: clonar(catalogoDB),
      totalOrden: orden.totalOrden
    }
  },

  async gestionarSolicitudPieza(ordenId, detalleId, { estado, cubiertoPorGarantia }) {
    const estadosPermitidos = ['APROBADA', 'RECHAZADA']
    if (!estadosPermitidos.includes(estado)) {
      throw new Error(`Estado de solicitud no válido: ${estado}`)
    }

    const orden = ordenesDB.find((o) => o.id === ordenId)
    if (!orden) {
      throw new Error(`No se encontró la orden ${ordenId}`)
    }
    if (orden.estado === 'ENTREGADA' || orden.estado === 'CANCELADA') {
      throw new Error(`La orden está en estado ${orden.estado} y es de solo lectura.`)
    }

    const item = orden.solicitudesPiezas.find((s) => s.id === detalleId)
    if (!item) {
      throw new Error('No se encontró la solicitud de pieza en la orden.')
    }

    const pieza = catalogoDB.find((p) => p.id === item.piezaId)
    const cantidad = Number(item.cantidad) || 0

    if (estado === 'APROBADA' && item.estado !== 'APROBADA') {
      const stockDisponible = pieza ? Number(pieza.stock) : Number(item.stockActual)
      if (stockDisponible < cantidad) {
        throw new Error(
          `Stock insuficiente para "${item.repuesto}" (Requerido: ${cantidad} u., Disponible: ${stockDisponible} u.).`
        )
      }
      if (pieza) {
        pieza.stock = stockDisponible - cantidad
        item.stockActual = pieza.stock
      }
      if (!item.precioUnitarioHistorico && pieza) {
        item.precioUnitarioHistorico = Number(pieza.precioVenta) || 0
      }
    }

    if (typeof cubiertoPorGarantia === 'boolean') {
      item.cubiertoPorGarantia = cubiertoPorGarantia
    }

    item.estado = estado
    orden.solicitudesPiezas.forEach((s) => {
      const p = catalogoDB.find((cat) => cat.id === s.piezaId)
      if (p) s.stockActual = p.stock
    })
    orden.totalOrden = calcularTotalOrden(orden)

    return {
      ordenId,
      solicitudesPiezas: clonar(orden.solicitudesPiezas),
      repuestos: clonar(catalogoDB),
      totalOrden: orden.totalOrden
    }
  },

  async actualizarManoObraOrden(ordenId, costoManoObra) {
    const orden = ordenesDB.find((o) => o.id === ordenId)
    if (!orden) {
      throw new Error(`No se encontró la orden ${ordenId}`)
    }
    if (orden.estado === 'ENTREGADA' || orden.estado === 'CANCELADA') {
      throw new Error(`La orden está en estado ${orden.estado} y es de solo lectura.`)
    }

    const valorManoObra = Number(costoManoObra)
    if (!Number.isFinite(valorManoObra) || valorManoObra < 0) {
      throw new Error('El costo de mano de obra debe ser un valor numérico igual o mayor a 0.')
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