import { ordenesInicialesMock, mecanicosMock } from '../data/ordenesMock'

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

export const ordenesService = {
  async obtenerOrdenes() {
    return clonar(ordenesDB)
  },

  async obtenerMecanicos() {
    return clonar(mecanicosMock)
  },

  async crearOrden(payload) {
    const nuevaOrden = {
      id: payload.id ?? `ORD-${String(ordenesDB.length + 1).padStart(3, '0')}`,
      cliente: payload.cliente,
      vehiculo: payload.vehiculo,
      placa: payload.placa,
      servicio: payload.servicio,
      mecanicoId: payload.mecanicoId ?? null,
      estado: ESTADOS_ORDEN_VALIDOS.includes(payload.estado) ? payload.estado : 'PENDIENTE',
      motivoCancelacion: null,
      fechaIngreso: payload.fechaIngreso ?? new Date().toISOString().slice(0, 10),
      garantia: clonar(
        payload.garantia ?? {
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
      ),
      solicitudesPiezas: clonar(payload.solicitudesPiezas ?? [])
    }

    ordenesDB.unshift(nuevaOrden)
    return clonar(nuevaOrden)
  },

  async actualizarGarantiaOrden(ordenId, garantiaPayload, marcarFinalizada = false) {
    const orden = ordenesDB.find((o) => o.id === ordenId)
    const nuevoEstado = marcarFinalizada ? 'FINALIZADA' : null

    if (orden) {
      orden.garantia = clonar(garantiaPayload)
      if (nuevoEstado && ESTADOS_ORDEN_VALIDOS.includes(nuevoEstado)) {
        orden.estado = nuevoEstado
      }
    }

    return {
      ordenId,
      garantia: clonar(garantiaPayload),
      nuevoEstado
    }
  },

  async actualizarEstadoOrden(ordenId, nuevoEstado, motivo = null) {
    if (!ESTADOS_ORDEN_VALIDOS.includes(nuevoEstado)) {
      throw new Error(`Estado de orden no válido: ${nuevoEstado}`)
    }

    const orden = ordenesDB.find((o) => o.id === ordenId)
    const motivoCancelacion = nuevoEstado === 'CANCELADA' ? motivo : null

    if (orden) {
      orden.estado = nuevoEstado
      orden.motivoCancelacion = motivoCancelacion
    }

    return {
      ordenId,
      nuevoEstado,
      motivoCancelacion
    }
  }
}