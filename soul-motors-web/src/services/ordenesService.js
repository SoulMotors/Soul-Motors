import { ordenesInicialesMock, mecanicosMock } from '../data/ordenesMock'

const clonar = (data) => JSON.parse(JSON.stringify(data))

export const ordenesService = {
  async obtenerOrdenes() {
    return clonar(ordenesInicialesMock)
  },

  async obtenerMecanicos() {
    return clonar(mecanicosMock)
  },

  async actualizarGarantiaOrden(ordenId, garantiaPayload, marcarControlCalidad = false) {
    return {
      ordenId,
      garantia: clonar(garantiaPayload),
      nuevoEstado: marcarControlCalidad ? 'Control de Calidad' : null
    }
  },

  async actualizarEstadoOrden(ordenId, nuevoEstado) {
    return { ordenId, nuevoEstado }
  }
}