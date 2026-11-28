<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-box">
      <div class="modal-head">
        <div>
          <span class="modal-tag">Bodega, Repuestos y Cobertura</span>
          <h3>Solicitud y Aprobación de Piezas — {{ orden.id }}</h3>
          <small class="head-sub">
            Estado de orden: <strong>{{ orden.estado }}</strong> · Vehículo: {{ orden.vehiculo }} ({{ orden.placa }})
          </small>
        </div>
        <button class="close-btn" @click="$emit('close')" title="Cerrar"><X :size="18" /></button>
      </div>

      <div class="modal-body">
        <div v-if="errorMsg" class="alert-error">
          {{ errorMsg }}
        </div>

        <div v-if="esSoloLectura" class="alert-readonly">
          Esta orden está en estado <strong>{{ orden.estado }}</strong>. No se pueden agregar ni modificar solicitudes de piezas.
        </div>

        <form v-else class="req-form" @submit.prevent="agregarSolicitud">
          <div class="field grow">
            <label>Repuesto en inventario</label>
            <select v-model="nuevo.piezaId" required>
              <option value="" disabled>Selecciona un repuesto...</option>
              <option v-for="r in repuestos" :key="r.id" :value="r.id">
                {{ r.nombre }} ({{ formatCOP(r.precioVenta) }} · Stock: {{ r.stock }} u.)
              </option>
            </select>
          </div>

          <div class="field qty">
            <label>Cantidad</label>
            <input type="number" min="1" v-model.number="nuevo.cantidad" required />
          </div>

          <div class="field grow">
            <label>Justificación / Nota</label>
            <input type="text" v-model="nuevo.nota" placeholder="Ej. Desgaste detectado o reclamo de garantía" />
          </div>

          <label class="check-garantia" title="Exime este repuesto del cobro si aplica cobertura de garantía">
            <input type="checkbox" v-model="nuevo.cubiertoPorGarantia" />
            <span>Cubierto garantía</span>
          </label>

          <button type="submit" class="btn-primary submit-btn" :disabled="enviando">
            <Plus :size="15" /> Solicitar
          </button>
        </form>

        <div class="summary-strip">
          <div class="sum-chip">
            <span>Piezas aprobadas (facturables)</span>
            <strong>{{ formatCOP(totalAprobadoFacturable) }}</strong>
          </div>
          <div class="sum-chip">
            <span>Exentas por garantía</span>
            <strong class="tag-green">{{ formatCOP(totalExentoGarantia) }}</strong>
          </div>
          <div class="sum-chip">
            <span>Solicitudes registradas</span>
            <strong>{{ solicitudes.length }}</strong>
          </div>
        </div>

        <div class="table-wrap">
          <table class="parts-table">
            <thead>
              <tr>
                <th>Repuesto Solicitado</th>
                <th>Cant.</th>
                <th>P. Unitario</th>
                <th>Subtotal</th>
                <th>Cobertura</th>
                <th>Estado</th>
                <th style="text-align: right;">Aprobación</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in solicitudes" :key="item.id">
                <td>
                  <strong>{{ item.repuesto }}</strong>
                  <small v-if="item.nota">{{ item.nota }}</small>
                  <small class="stock-hint">Stock disponible: {{ item.stockActual }} u.</small>
                </td>
                <td>{{ item.cantidad }} u.</td>
                <td>{{ formatCOP(item.precioUnitarioHistorico ?? item.precioVenta ?? 0) }}</td>
                <td>
                  <strong>
                    {{
                      item.cubiertoPorGarantia || ordenCubiertaCompleta
                        ? '$0'
                        : formatCOP((item.precioUnitarioHistorico ?? item.precioVenta ?? 0) * item.cantidad)
                    }}
                  </strong>
                </td>
                <td>
                  <button
                    type="button"
                    class="cov-pill"
                    :class="{ covered: item.cubiertoPorGarantia || ordenCubiertaCompleta }"
                    :disabled="esSoloLectura || !esAdministrador || item.estado === 'REVERTIDA' || ordenCubiertaCompleta || enviando"
                    @click="alternarCobertura(item)"
                  >
                    {{ item.cubiertoPorGarantia || ordenCubiertaCompleta ? 'Garantía' : 'Normal' }}
                  </button>
                </td>
                <td>
                  <span class="badge" :class="slugEstado(item.estado)">{{ item.estado }}</span>
                </td>
                <td class="actions-cell">
                  <button
                    v-if="esAdministrador"
                    type="button"
                    class="act-btn approve"
                    title="Aprobar pieza y descontar stock"
                    :disabled="esSoloLectura || item.estado === 'APROBADA' || item.estado === 'REVERTIDA' || enviando"
                    @click="cambiarEstado(item, 'APROBADA')"
                  >
                    <Check :size="14" /> Aprobar
                  </button>
                  <button
                    v-if="esAdministrador"
                    type="button"
                    class="act-btn reject"
                    title="Rechazar solicitud de pieza"
                    :disabled="esSoloLectura || item.estado === 'APROBADA' || item.estado === 'RECHAZADA' || item.estado === 'REVERTIDA' || enviando"
                    @click="cambiarEstado(item, 'RECHAZADA')"
                  >
                    <X :size="14" />
                  </button>
                  <span v-if="!esAdministrador" class="role-hint">Solo ADMIN</span>
                </td>
              </tr>
              <tr v-if="!solicitudes.length">
                <td colspan="7" class="empty-cell">No hay solicitudes de piezas registradas en esta orden.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="modal-foot">
        <button type="button" class="btn-secondary" @click="$emit('close')">Cerrar</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { X, Plus, Check } from 'lucide-vue-next'
import { ordenesService, ESTADOS_ORDEN_BLOQUEADOS_PIEZAS } from '../services/ordenesService'

const props = defineProps({ orden: { type: Object, required: true } })
const emit = defineEmits(['close', 'update-parts'])

const repuestos = ref([])
const solicitudes = ref(JSON.parse(JSON.stringify(props.orden.solicitudesPiezas || [])))
const errorMsg = ref('')
const enviando = ref(false)

const nuevo = reactive({
  piezaId: '',
  cantidad: 1,
  nota: '',
  cubiertoPorGarantia: false
})

const resolverRolSesion = () => {
  try {
    const rawUser = localStorage.getItem('sm_user')
    if (rawUser) {
      const parsed = JSON.parse(rawUser)
      const rol = parsed?.rol || parsed?.role
      if (rol) return String(rol).toUpperCase()
      const correo = String(parsed?.email || parsed?.correo || '').toLowerCase()
      if (correo.includes('admin')) return 'ADMINISTRADOR'
      if (correo.includes('mecanico')) return 'MECANICO'
      if (correo.includes('vendedor')) return 'VENDEDOR'
      if (correo.includes('gerente')) return 'GERENTE'
    }
    const rolDirecto = localStorage.getItem('sm_role')
    if (rolDirecto) return String(rolDirecto).toUpperCase()
  } catch {
    // Ignora error de lectura en storage
  }
  return 'ADMINISTRADOR'
}

const esAdministrador = computed(() => resolverRolSesion() === 'ADMINISTRADOR')

watch(
  () => props.orden,
  (nuevaOrden) => {
    solicitudes.value = JSON.parse(JSON.stringify(nuevaOrden?.solicitudesPiezas || []))
    errorMsg.value = ''
  },
  { deep: true }
)

onMounted(async () => {
  repuestos.value = await ordenesService.obtenerRepuestos()
})

const esSoloLectura = computed(() =>
  ESTADOS_ORDEN_BLOQUEADOS_PIEZAS.includes(props.orden.estado)
)

const ordenCubiertaCompleta = computed(
  () => Boolean(props.orden.ordenPadreGarantiaId) && props.orden.garantia?.estadoGarantia === 'RECLAMADA'
)

const totalAprobadoFacturable = computed(() =>
  solicitudes.value.reduce((acc, item) => {
    const cubierta = Boolean(item.cubiertoPorGarantia) || ordenCubiertaCompleta.value
    if (item.estado !== 'APROBADA' || cubierta) return acc
    const precio = Number(item.precioUnitarioHistorico ?? item.precioVenta ?? 0)
    return acc + precio * (Number(item.cantidad) || 0)
  }, 0)
)

const totalExentoGarantia = computed(() =>
  solicitudes.value.reduce((acc, item) => {
    const cubierta = Boolean(item.cubiertoPorGarantia) || ordenCubiertaCompleta.value
    if (item.estado !== 'APROBADA' || !cubierta) return acc
    const precio = Number(item.precioUnitarioHistorico ?? item.precioVenta ?? 0)
    return acc + precio * (Number(item.cantidad) || 0)
  }, 0)
)

const formatCOP = (valor) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0
  }).format(Number(valor) || 0)

const slugEstado = (estado) => (estado || '').toLowerCase().replace(/[\s_]+/g, '-')

const agregarSolicitud = async () => {
  errorMsg.value = ''
  if (!nuevo.piezaId) return

  enviando.value = true
  try {
    const res = await ordenesService.solicitarPiezaOrden(props.orden.id, {
      piezaId: nuevo.piezaId,
      cantidad: nuevo.cantidad,
      nota: nuevo.nota,
      cubiertoPorGarantia: nuevo.cubiertoPorGarantia
    })
    solicitudes.value = res.solicitudesPiezas
    if (res.repuestos) repuestos.value = res.repuestos
    nuevo.piezaId = ''
    nuevo.cantidad = 1
    nuevo.nota = ''
    nuevo.cubiertoPorGarantia = false
    emit('update-parts', {
      ordenId: props.orden.id,
      solicitudes: solicitudes.value,
      totalOrden: res.totalOrden
    })
  } catch (err) {
    errorMsg.value = err?.message || 'No se pudo registrar la solicitud de pieza.'
  } finally {
    enviando.value = false
  }
}

const cambiarEstado = async (item, estado) => {
  if (!esAdministrador.value) {
    errorMsg.value = 'Solo el rol ADMINISTRADOR puede aprobar o rechazar piezas.'
    return
  }
  if (estado === 'RECHAZADA' && item.estado === 'APROBADA') {
    errorMsg.value = 'No se puede rechazar una pieza que ya está APROBADA.'
    return
  }

  errorMsg.value = ''
  enviando.value = true
  try {
    const res = await ordenesService.gestionarSolicitudPieza(props.orden.id, item.id, {
      estado,
      cubiertoPorGarantia: item.cubiertoPorGarantia
    })
    solicitudes.value = res.solicitudesPiezas
    if (res.repuestos) repuestos.value = res.repuestos
    emit('update-parts', {
      ordenId: props.orden.id,
      solicitudes: solicitudes.value,
      totalOrden: res.totalOrden
    })
  } catch (err) {
    errorMsg.value = err?.message || 'No se pudo actualizar la solicitud.'
  } finally {
    enviando.value = false
  }
}

const alternarCobertura = async (item) => {
  if (!esAdministrador.value) return
  errorMsg.value = ''
  enviando.value = true
  try {
    const nuevoValor = !item.cubiertoPorGarantia
    const estadoObjetivo = item.estado === 'APROBADA' ? 'APROBADA' : (item.estado === 'RECHAZADA' ? 'RECHAZADA' : null)
    if (estadoObjetivo) {
      const res = await ordenesService.gestionarSolicitudPieza(props.orden.id, item.id, {
        estado: estadoObjetivo,
        cubiertoPorGarantia: nuevoValor
      })
      solicitudes.value = res.solicitudesPiezas
      emit('update-parts', {
        ordenId: props.orden.id,
        solicitudes: solicitudes.value,
        totalOrden: res.totalOrden
      })
    } else {
      item.cubiertoPorGarantia = nuevoValor
      emit('update-parts', {
        ordenId: props.orden.id,
        solicitudes: solicitudes.value
      })
    }
  } catch (err) {
    errorMsg.value = err?.message || 'No se pudo actualizar la cobertura de garantía.'
  } finally {
    enviando.value = false
  }
}
</script>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.65); display: flex; align-items: center; justify-content: center; z-index: 50; padding: 20px; }
.modal-box { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; width: 100%; max-width: 880px; max-height: 90vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4); }
.modal-head { display: flex; justify-content: space-between; align-items: flex-start; padding: 18px 24px; border-bottom: 1px solid var(--border-color); }
.modal-tag { font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #E67E22; font-weight: 700; }
.modal-head h3 { margin: 4px 0 0; font-size: 17px; }
.head-sub { display: block; margin-top: 4px; font-size: 12px; color: var(--text-secondary); }
.close-btn { background: transparent; border: none; color: var(--text-secondary); cursor: pointer; padding: 4px; }
.modal-body { padding: 18px 24px; display: flex; flex-direction: column; gap: 16px; overflow-y: auto; }
.alert-error { background: rgba(231, 76, 60, 0.14); border: 1px solid rgba(231, 76, 60, 0.4); color: #E74C3C; padding: 10px 14px; border-radius: 10px; font-size: 12px; font-weight: 600; }
.alert-readonly { background: rgba(52, 152, 219, 0.14); border: 1px solid rgba(52, 152, 219, 0.35); color: var(--text-primary); padding: 10px 14px; border-radius: 10px; font-size: 12px; }
.req-form { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 10px; background: var(--input-bg); padding: 14px; border-radius: 12px; border: 1px solid var(--border-color); }
.field { display: flex; flex-direction: column; gap: 5px; }
.field.grow { flex: 1; min-width: 170px; }
.field.qty { width: 84px; }
.field label { font-size: 11px; color: var(--text-secondary); font-weight: 600; }
.field select, .field input { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 8px 10px; color: var(--text-primary); font-size: 12px; outline: none; height: 36px; box-sizing: border-box; }
.field select:focus, .field input:focus { border-color: #E67E22; }
.check-garantia { display: inline-flex; align-items: center; gap: 6px; height: 36px; padding: 0 10px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-card); font-size: 12px; color: var(--text-primary); cursor: pointer; white-space: nowrap; }
.submit-btn { height: 36px; padding: 0 14px; }
.summary-strip { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.sum-chip { background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 10px 14px; display: flex; flex-direction: column; gap: 3px; }
.sum-chip span { font-size: 11px; color: var(--text-secondary); }
.sum-chip strong { font-size: 14px; }
.tag-green { color: #2ECC71; }
.table-wrap { border: 1px solid var(--border-color); border-radius: 12px; overflow-x: auto; }
.parts-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.parts-table th { text-align: left; padding: 10px 12px; background: var(--input-bg); color: var(--text-secondary); font-weight: 600; border-bottom: 1px solid var(--border-color); white-space: nowrap; }
.parts-table td { padding: 10px 12px; border-bottom: 1px solid var(--border-color); vertical-align: middle; }
.parts-table td strong { display: block; font-size: 12px; }
.parts-table td small { display: block; color: var(--text-secondary); font-size: 11px; }
.stock-hint { margin-top: 2px; font-size: 10px; opacity: 0.85; }
.cov-pill { border: 1px solid var(--border-color); background: var(--input-bg); color: var(--text-secondary); padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 600; cursor: pointer; }
.cov-pill.covered { background: rgba(46, 204, 113, 0.16); border-color: rgba(46, 204, 113, 0.45); color: #2ECC71; }
.cov-pill:disabled { opacity: 0.65; cursor: default; }
.badge { padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; white-space: nowrap; }
.badge.solicitada, .badge.pendiente { background: rgba(241, 196, 15, 0.15); color: #F1C40F; }
.badge.aprobada { background: rgba(46, 204, 113, 0.15); color: #2ECC71; }
.badge.rechazada, .badge.revertida { background: rgba(231, 76, 60, 0.15); color: #E74C3C; }
.actions-cell { display: flex; justify-content: flex-end; gap: 6px; white-space: nowrap; }
.act-btn { border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); padding: 5px 9px; border-radius: 7px; font-size: 11px; cursor: pointer; display: inline-flex; align-items: center; gap: 4px; }
.act-btn.approve:hover:not(:disabled) { border-color: #2ECC71; color: #2ECC71; }
.act-btn.reject:hover:not(:disabled) { border-color: #E74C3C; color: #E74C3C; }
.act-btn:disabled { opacity: 0.35; cursor: default; }
.role-hint { font-size: 11px; color: var(--text-secondary); font-weight: 600; }
.empty-cell { text-align: center; color: var(--text-secondary); padding: 20px; }
.modal-foot { display: flex; justify-content: flex-end; padding: 14px 24px; border-top: 1px solid var(--border-color); }
.btn-secondary { background: transparent; border: 1px solid var(--border-color); color: var(--text-primary); padding: 8px 16px; border-radius: 10px; font-size: 12px; cursor: pointer; }
.btn-primary { background: #E67E22; border: none; color: #fff; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; }
.btn-primary:disabled { opacity: 0.5; cursor: default; }
</style>