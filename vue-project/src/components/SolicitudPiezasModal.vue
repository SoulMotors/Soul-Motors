<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-box">
      <div class="modal-head">
        <div>
          <span class="modal-tag">Bodega y Repuestos</span>
          <h3>Solicitud y Aprobación de Piezas — {{ orden.id }}</h3>
        </div>
        <button class="close-btn" @click="$emit('close')"><X :size="18" /></button>
      </div>

      <div class="modal-body">
        <form class="req-form" @submit.prevent="agregarSolicitud">
          <div class="field grow">
            <label>Repuesto en inventario</label>
            <select v-model="nuevo.repuestoId" required>
              <option value="" disabled>Selecciona un repuesto...</option>
              <option v-for="r in repuestos" :key="r.id" :value="r.id">
                {{ r.nombre }} (Stock: {{ r.stock }} u.)
              </option>
            </select>
          </div>

          <div class="field qty">
            <label>Cantidad</label>
            <input type="number" min="1" v-model.number="nuevo.cantidad" required />
          </div>

          <div class="field grow">
            <label>Justificación / Nota</label>
            <input type="text" v-model="nuevo.nota" placeholder="Ej. Desgaste detectado" />
          </div>

          <button type="submit" class="btn-primary submit-btn">
            <Plus :size="15" /> Solicitar
          </button>
        </form>

        <div class="table-wrap">
          <table class="parts-table">
            <thead>
              <tr>
                <th>Repuesto Solicitado</th>
                <th>Cant.</th>
                <th>Stock</th>
                <th>Estado</th>
                <th style="text-align: right;">Aprobación</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in solicitudes" :key="item.id">
                <td>
                  <strong>{{ item.repuesto }}</strong>
                  <small v-if="item.nota">{{ item.nota }}</small>
                </td>
                <td>{{ item.cantidad }} u.</td>
                <td>{{ item.stockActual }} u.</td>
                <td>
                  <span class="badge" :class="item.estado.toLowerCase()">{{ item.estado }}</span>
                </td>
                <td class="actions-cell">
                  <button
                    class="act-btn approve"
                    title="Aprobar pieza"
                    :disabled="item.estado === 'Aprobada'"
                    @click="cambiarEstado(item, 'Aprobada')"
                  >
                    <Check :size="14" /> Aprobar
                  </button>
                  <button
                    class="act-btn reject"
                    title="Rechazar pieza"
                    :disabled="item.estado === 'Rechazada'"
                    @click="cambiarEstado(item, 'Rechazada')"
                  >
                    <X :size="14" />
                  </button>
                </td>
              </tr>
              <tr v-if="!solicitudes.length">
                <td colspan="5" class="empty-cell">No hay solicitudes de piezas registradas en esta orden.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="modal-foot">
        <button class="btn-secondary" @click="$emit('close')">Cerrar</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { X, Plus, Check } from 'lucide-vue-next'
import { repuestosDisponiblesMock } from '../data/ordenesMock'

const props = defineProps({ orden: { type: Object, required: true } })
const emit = defineEmits(['close', 'update-parts'])

const repuestos = ref(repuestosDisponiblesMock)
const solicitudes = ref([...props.orden.solicitudesPiezas])

const nuevo = reactive({ repuestoId: '', cantidad: 1, nota: '' })

const agregarSolicitud = () => {
  const rep = repuestos.value.find(r => r.id === nuevo.repuestoId)
  if (!rep) return
  solicitudes.value.unshift({
    id: Date.now(),
    repuesto: rep.nombre,
    cantidad: nuevo.cantidad,
    stockActual: rep.stock,
    estado: 'Pendiente',
    nota: nuevo.nota || 'Solicitud desde taller'
  })
  nuevo.repuestoId = ''
  nuevo.cantidad = 1
  nuevo.nota = ''
  emit('update-parts', { ordenId: props.orden.id, solicitudes: solicitudes.value })
}

const cambiarEstado = (item, estado) => {
  item.estado = estado
  emit('update-parts', { ordenId: props.orden.id, solicitudes: solicitudes.value })
}
</script>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.65); display: flex; align-items: center; justify-content: center; z-index: 50; padding: 20px; }
.modal-box { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; width: 100%; max-width: 740px; overflow: hidden; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4); }
.modal-head { display: flex; justify-content: space-between; align-items: flex-start; padding: 20px 24px; border-bottom: 1px solid var(--border-color); }
.modal-tag { font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #E67E22; font-weight: 700; }
.modal-head h3 { margin: 4px 0 0; font-size: 17px; }
.close-btn { background: transparent; border: none; color: var(--text-secondary); cursor: pointer; padding: 4px; }
.modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 18px; }
.req-form { display: flex; align-items: flex-end; gap: 10px; background: var(--input-bg); padding: 14px; border-radius: 12px; border: 1px solid var(--border-color); }
.field { display: flex; flex-direction: column; gap: 5px; }
.field.grow { flex: 1; }
.field.qty { width: 80px; }
.field label { font-size: 11px; color: var(--text-secondary); font-weight: 600; }
.field select, .field input { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 8px 10px; color: var(--text-primary); font-size: 12px; outline: none; height: 36px; box-sizing: border-box; }
.submit-btn { height: 36px; padding: 0 14px; }
.table-wrap { border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; }
.parts-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.parts-table th { text-align: left; padding: 10px 14px; background: var(--input-bg); color: var(--text-secondary); font-weight: 600; border-bottom: 1px solid var(--border-color); }
.parts-table td { padding: 11px 14px; border-bottom: 1px solid var(--border-color); vertical-align: middle; }
.parts-table td strong { display: block; font-size: 13px; }
.parts-table td small { color: var(--text-secondary); font-size: 11px; }
.badge { padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
.badge.pendiente { background: rgba(241, 196, 15, 0.15); color: #F1C40F; }
.badge.aprobada { background: rgba(46, 204, 113, 0.15); color: #2ECC71; }
.badge.rechazada { background: rgba(231, 76, 60, 0.15); color: #E74C3C; }
.actions-cell { display: flex; justify-content: flex-end; gap: 6px; }
.act-btn { border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); padding: 5px 9px; border-radius: 7px; font-size: 11px; cursor: pointer; display: inline-flex; align-items: center; gap: 4px; }
.act-btn.approve:hover:not(:disabled) { border-color: #2ECC71; color: #2ECC71; }
.act-btn.reject:hover:not(:disabled) { border-color: #E74C3C; color: #E74C3C; }
.act-btn:disabled { opacity: 0.35; cursor: default; }
.empty-cell { text-align: center; color: var(--text-secondary); padding: 20px; }
.modal-foot { display: flex; justify-content: flex-end; padding: 14px 24px; border-top: 1px solid var(--border-color); }
.btn-secondary { background: transparent; border: 1px solid var(--border-color); color: var(--text-primary); padding: 8px 16px; border-radius: 10px; font-size: 12px; cursor: pointer; }
.btn-primary { background: #E67E22; border: none; color: #fff; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; }
</style>