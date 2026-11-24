<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-box">
      <div class="modal-head">
        <div>
          <span class="modal-tag">Control de Calidad y Garantía</span>
          <h3>Orden {{ orden.id }} — {{ orden.vehiculo }} ({{ orden.placa }})</h3>
        </div>
        <button class="close-btn" @click="$emit('close')"><X :size="18" /></button>
      </div>

      <div class="modal-body">
        <div class="section-block">
          <h4>Checklist Técnico de Trabajos Realizados</h4>
          <p class="hint">Verifica los 5 puntos críticos antes de certificar la garantía de entrega.</p>

          <div class="checklist-grid">
            <label class="check-item" :class="{ checked: form.checklist.torque }">
              <input type="checkbox" v-model="form.checklist.torque" />
              <div>
                <strong>Torque y Ajuste Mecánico</strong>
                <span>Verificación de par de apriete en tornillería crítica</span>
              </div>
            </label>

            <label class="check-item" :class="{ checked: form.checklist.frenos }">
              <input type="checkbox" v-model="form.checklist.frenos" />
              <div>
                <strong>Sistema de Frenos</strong>
                <span>Pastillas, discos, nivel de líquido y purga</span>
              </div>
            </label>

            <label class="check-item" :class="{ checked: form.checklist.sistemaElectrico }">
              <input type="checkbox" v-model="form.checklist.sistemaElectrico" />
              <div>
                <strong>Sistema Eléctrico</strong>
                <span>Carga de batería, luces, tablero y sensores</span>
              </div>
            </label>

            <label class="check-item" :class="{ checked: form.checklist.pruebaRuta }">
              <input type="checkbox" v-model="form.checklist.pruebaRuta" />
              <div>
                <strong>Prueba de Ruta</strong>
                <span>Recorrido dinámico sin ruidos ni vibraciones</span>
              </div>
            </label>

            <label class="check-item check-item-full" :class="{ checked: form.checklist.estanqueidadEmpaques }">
              <input type="checkbox" v-model="form.checklist.estanqueidadEmpaques" />
              <div>
                <strong>Estanqueidad de Empaques</strong>
                <span>Ausencia de fugas en zonas intervenidas</span>
              </div>
            </label>
          </div>
        </div>

        <div class="section-block">
          <h4>Cobertura y Datos de Entrega</h4>
          <div class="coverage-row">
            <div class="field">
              <label>Cobertura en Días</label>
              <div class="input-unit">
                <input type="number" min="0" v-model.number="form.dias" />
                <span>días</span>
              </div>
            </div>
            <div class="field">
              <label>Cobertura en Kilómetros</label>
              <div class="input-unit">
                <input type="number" min="0" step="100" v-model.number="form.kilometros" />
                <span>km</span>
              </div>
            </div>
          </div>

          <div class="coverage-row" style="margin-top: 12px;">
            <div class="field">
              <label>Kilometraje de Entrega</label>
              <div class="input-unit">
                <input type="number" min="0" v-model.number="form.kmEntrega" placeholder="Ej. 45200" />
                <span>km</span>
              </div>
            </div>
            <div class="field">
              <label>Series/Lotes de Piezas</label>
              <input
                type="text"
                class="text-input"
                v-model="form.seriesLotesPiezas"
                placeholder="Ej. LOTE-A12, SER-9981"
              />
            </div>
          </div>

          <div class="field" style="margin-top: 12px;">
            <label>Observaciones Técnicas</label>
            <textarea v-model="form.observaciones" rows="2" placeholder="Notas del control de calidad..."></textarea>
          </div>
        </div>
      </div>

      <div class="modal-foot">
        <span class="status-note" :class="todosMarcados ? 'ok' : 'warn'">
          {{ todosMarcados ? 'Checklist completo (5/5)' : 'Faltan puntos por verificar en el checklist (0-5/5)' }}
        </span>
        <div class="foot-actions">
          <button class="btn-secondary" @click="$emit('close')">Cancelar</button>
          <button class="btn-primary" :disabled="!todosMarcados" @click="guardar">
            <ShieldCheck :size="16" /> Guardar y Certificar Garantía
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { X, ShieldCheck } from 'lucide-vue-next'

const props = defineProps({ orden: { type: Object, required: true } })
const emit = defineEmits(['close', 'save'])

const garantiaActual = props.orden?.garantia ?? {}
const checklistActual = garantiaActual.checklist ?? {}

const form = reactive({
  checklist: {
    torque: Boolean(checklistActual.torque),
    frenos: Boolean(checklistActual.frenos),
    sistemaElectrico: Boolean(checklistActual.sistemaElectrico),
    pruebaRuta: Boolean(checklistActual.pruebaRuta),
    estanqueidadEmpaques: Boolean(checklistActual.estanqueidadEmpaques)
  },
  dias: garantiaActual.dias ?? 30,
  kilometros: garantiaActual.kilometros ?? 0,
  kmEntrega: garantiaActual.kmEntrega ?? '',
  seriesLotesPiezas: garantiaActual.seriesLotesPiezas ?? '',
  observaciones: garantiaActual.observaciones ?? ''
})

const todosMarcados = computed(() =>
  form.checklist.torque &&
  form.checklist.frenos &&
  form.checklist.sistemaElectrico &&
  form.checklist.pruebaRuta &&
  form.checklist.estanqueidadEmpaques
)

const guardar = () => {
  if (!todosMarcados.value) return

  emit('save', {
    ordenId: props.orden.id,
    garantia: {
      checklist: { ...form.checklist },
      dias: form.dias ?? 30,
      kilometros: form.kilometros ?? 0,
      kmEntrega: form.kmEntrega,
      seriesLotesPiezas: form.seriesLotesPiezas,
      observaciones: form.observaciones,
      certificada: true
    }
  })
}
</script>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.65); display: flex; align-items: center; justify-content: center; z-index: 50; padding: 20px; }
.modal-box { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; width: 100%; max-width: 680px; max-height: 90vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4); }
.modal-head { display: flex; justify-content: space-between; align-items: flex-start; padding: 20px 24px; border-bottom: 1px solid var(--border-color); }
.modal-tag { font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #E67E22; font-weight: 700; }
.modal-head h3 { margin: 4px 0 0; font-size: 17px; }
.close-btn { background: transparent; border: none; color: var(--text-secondary); cursor: pointer; padding: 4px; }
.modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 20px; overflow-y: auto; }
.section-block h4 { margin: 0 0 4px; font-size: 14px; font-weight: 600; }
.hint { margin: 0 0 12px; font-size: 12px; color: var(--text-secondary); }
.checklist-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.check-item { display: flex; align-items: flex-start; gap: 10px; padding: 12px; border-radius: 10px; border: 1px solid var(--border-color); background: var(--input-bg); cursor: pointer; transition: 0.2s; }
.check-item-full { grid-column: 1 / -1; }
.check-item.checked { border-color: #2ECC71; background: rgba(46, 204, 113, 0.08); }
.check-item input { accent-color: #2ECC71; width: 16px; height: 16px; margin-top: 2px; flex-shrink: 0; }
.check-item strong { display: block; font-size: 13px; }
.check-item span { font-size: 11px; color: var(--text-secondary); }
.coverage-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 10px; }
.field label { display: block; font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
.input-unit { display: flex; align-items: center; background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding-right: 12px; }
.input-unit input { flex: 1; background: transparent; border: none; color: var(--text-primary); padding: 9px 12px; font-size: 13px; outline: none; }
.input-unit span { font-size: 12px; color: var(--text-secondary); font-weight: 600; }
.text-input { width: 100%; box-sizing: border-box; background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 9px 12px; color: var(--text-primary); font-size: 13px; outline: none; }
textarea { width: 100%; box-sizing: border-box; background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 10px 12px; color: var(--text-primary); font-size: 13px; outline: none; resize: none; }
.modal-foot { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-top: 1px solid var(--border-color); gap: 12px; }
.status-note { font-size: 12px; font-weight: 600; }
.status-note.ok { color: #2ECC71; }
.status-note.warn { color: #E67E22; }
.foot-actions { display: flex; gap: 10px; }
.btn-secondary { background: transparent; border: 1px solid var(--border-color); color: var(--text-primary); padding: 8px 14px; border-radius: 10px; font-size: 12px; cursor: pointer; }
.btn-primary { background: #E67E22; border: none; color: #fff; padding: 8px 16px; border-radius: 10px; font-size: 12px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: opacity 0.2s; }
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }
</style>