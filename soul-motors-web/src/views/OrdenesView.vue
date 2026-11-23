<template>
  <AppShell
    :isDarkMode="isDarkMode"
    :navItems="navItems"
    activeKey="ordenes"
    title="Gestión de Órdenes de Servicio"
    subtitle="Asignación de mecánicos por carga, control técnico de calidad y solicitud de repuestos"
    @navigate="$emit('change-view', $event)"
    @toggle-theme="$emit('toggle-theme')"
    @logout="$emit('change-view', 'login')"
  >
    <section class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <h3>Órdenes Activas en Taller</h3>
          <span class="pill-count">{{ ordenesFiltradas.length }} órdenes</span>
        </div>

        <div class="toolbar-right">
          <div class="search-box">
            <Search :size="15" class="search-ico" />
            <input v-model="busqueda" type="text" placeholder="Buscar placa, cliente u orden..." />
          </div>

          <select v-model="filtroEstado" class="filter-select">
            <option value="">Todos los estados</option>
            <option value="Pendiente">Pendiente</option>
            <option value="En Proceso">En Proceso</option>
            <option value="Control de Calidad">Control de Calidad</option>
          </select>
        </div>
      </div>

      <div class="table-container">
        <table class="orders-table">
          <thead>
            <tr>
              <th>N° Orden</th>
              <th>Cliente y Vehículo</th>
              <th>Servicio Solicitado</th>
              <th>Mecánico Asignado (Carga)</th>
              <th>Estado</th>
              <th style="text-align: right;">Gestión Técnica</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="orden in ordenesFiltradas" :key="orden.id">
              <td>
                <strong class="order-id">{{ orden.id }}</strong>
                <small class="sub-text">{{ orden.fechaIngreso }}</small>
              </td>

              <td>
                <strong>{{ orden.cliente }}</strong>
                <small class="sub-text">{{ orden.vehiculo }} · <span class="plate">{{ orden.placa }}</span></small>
              </td>

              <td>
                <span class="service-text">{{ orden.servicio }}</span>
                <div class="mini-tags">
                  <span class="mini-tag" :class="{ certified: orden.garantia.certificada }">
                    <ShieldCheck :size="12" />
                    {{ orden.garantia.certificada ? `Garantía ${orden.garantia.dias}d / ${orden.garantia.kilometros}km` : 'Garantía pendiente' }}
                  </span>
                  <span class="mini-tag">
                    <Wrench :size="12" /> {{ orden.solicitudesPiezas.length }} pieza(s)
                  </span>
                </div>
              </td>

              <!-- Desplegable interactivo con barra de carga de mecánicos -->
              <td class="mech-cell">
                <div class="mech-dropdown">
                  <button class="mech-trigger" @click="toggleDropdown(orden.id)">
                    <div class="mech-info">
                      <strong>{{ getMecanico(orden.mecanicoId)?.nombre }}</strong>
                      <div class="load-bar">
                        <div
                          class="load-fill"
                          :style="{
                            width: getPorcentajeCarga(getMecanico(orden.mecanicoId)) + '%',
                            backgroundColor: getColorCarga(getMecanico(orden.mecanicoId))
                          }"
                        ></div>
                      </div>
                    </div>
                    <span class="load-ratio">
                      {{ getMecanico(orden.mecanicoId)?.ordenesActivas }}/{{ getMecanico(orden.mecanicoId)?.capacidadMaxima }}
                    </span>
                    <ChevronDown :size="15" />
                  </button>

                  <div v-if="dropdownAbierto === orden.id" class="mech-menu">
                    <div class="mech-menu-header">Seleccionar mecánico según carga</div>
                    <button
                      v-for="mec in mecanicos"
                      :key="mec.id"
                      class="mech-option"
                      :class="{ selected: mec.id === orden.mecanicoId }"
                      @click="asignarMecanico(orden, mec.id)"
                    >
                      <div class="opt-top">
                        <strong>{{ mec.nombre }}</strong>
                        <span class="opt-badge" :style="{ color: getColorCarga(mec) }">
                          {{ mec.ordenesActivas }}/{{ mec.capacidadMaxima }} órdenes ({{ getPorcentajeCarga(mec) }}%)
                        </span>
                      </div>
                      <small>{{ mec.especialidad }}</small>
                      <div class="load-bar full">
                        <div
                          class="load-fill"
                          :style="{ width: getPorcentajeCarga(mec) + '%', backgroundColor: getColorCarga(mec) }"
                        ></div>
                      </div>
                    </button>
                  </div>
                </div>
              </td>

              <td>
                <span class="status-badge" :class="slugEstado(orden.estado)">{{ orden.estado }}</span>
              </td>

              <td class="actions-col">
                <button class="action-btn" @click="ordenGarantia = orden" title="Checklist Técnico y Garantía">
                  <ClipboardCheck :size="15" /> Garantía
                </button>
                <button class="action-btn orange" @click="ordenPiezas = orden" title="Solicitar o Aprobar Piezas">
                  <Cog :size="15" /> Piezas
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <GarantiaChecklistModal
      v-if="ordenGarantia"
      :orden="ordenGarantia"
      @close="ordenGarantia = null"
      @save="actualizarGarantia"
    />

    <SolicitudPiezasModal
      v-if="ordenPiezas"
      :orden="ordenPiezas"
      @close="ordenPiezas = null"
      @update-parts="actualizarPiezas"
    />
  </AppShell>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  LayoutDashboard, Package, Users, ClipboardList,
  Search, ChevronDown, ShieldCheck, Wrench, ClipboardCheck, Cog
} from 'lucide-vue-next'
import AppShell from '../components/AppShell.vue'
import GarantiaChecklistModal from '../components/GarantiaChecklistModal.vue'
import SolicitudPiezasModal from '../components/SolicitudPiezasModal.vue'
import { ordenesInicialesMock, mecanicosMock } from '../data/ordenesMock'

defineProps({ isDarkMode: Boolean })
defineEmits(['change-view', 'toggle-theme'])

const navItems = [
  { key: 'gerente', label: 'Gerencia', icon: LayoutDashboard },
  { key: 'ordenes', label: 'Órdenes', icon: ClipboardList },
  { key: 'inventario', label: 'Inventario', icon: Package },
  { key: 'trabajadores', label: 'Trabajadores', icon: Users }
]

const ordenes = ref(ordenesInicialesMock)
const mecanicos = ref(mecanicosMock)
const busqueda = ref('')
const filtroEstado = ref('')
const dropdownAbierto = ref(null)
const ordenGarantia = ref(null)
const ordenPiezas = ref(null)

const ordenesFiltradas = computed(() => {
  const q = busqueda.value.trim().toLowerCase()
  return ordenes.value.filter(o => {
    const matchEstado = !filtroEstado.value || o.estado === filtroEstado.value
    const matchTexto = !q ||
      o.id.toLowerCase().includes(q) ||
      o.cliente.toLowerCase().includes(q) ||
      o.placa.toLowerCase().includes(q) ||
      o.vehiculo.toLowerCase().includes(q)
    return matchEstado && matchTexto
  })
})

const getMecanico = (id) => mecanicos.value.find(m => m.id === id)
const getPorcentajeCarga = (mec) => mec ? Math.round((mec.ordenesActivas / mec.capacidadMaxima) * 100) : 0
const getColorCarga = (mec) => {
  const pct = getPorcentajeCarga(mec)
  if (pct >= 80) return '#E74C3C'
  if (pct >= 50) return '#F39C12'
  return '#2ECC71'
}

const toggleDropdown = (ordenId) => {
  dropdownAbierto.value = dropdownAbierto.value === ordenId ? null : ordenId
}

const asignarMecanico = (orden, nuevoMecanicoId) => {
  if (orden.mecanicoId !== nuevoMecanicoId) {
    const anterior = getMecanico(orden.mecanicoId)
    const nuevo = getMecanico(nuevoMecanicoId)
    if (anterior && anterior.ordenesActivas > 0) anterior.ordenesActivas--
    if (nuevo) nuevo.ordenesActivas++
    orden.mecanicoId = nuevoMecanicoId
  }
  dropdownAbierto.value = null
}

const actualizarGarantia = ({ ordenId, garantia }) => {
  const target = ordenes.value.find(o => o.id === ordenId)
  if (target) target.garantia = garantia
  ordenGarantia.value = null
}

const actualizarPiezas = ({ ordenId, solicitudes }) => {
  const target = ordenes.value.find(o => o.id === ordenId)
  if (target) target.solicitudesPiezas = solicitudes
}

const slugEstado = (estado) => estado.toLowerCase().replace(/\s+/g, '-')
</script>

<style scoped>
.card { background-color: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; padding: 24px; display: flex; flex-direction: column; gap: 20px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-left h3 { margin: 0; font-size: 17px; font-weight: 600; }
.pill-count { background: rgba(230, 126, 34, 0.15); color: #E67E22; font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 999px; }
.toolbar-right { display: flex; gap: 10px; align-items: center; }
.search-box { position: relative; display: flex; align-items: center; }
.search-ico { position: absolute; left: 12px; color: var(--text-secondary); }
.search-box input { background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 8px 14px 8px 34px; color: var(--text-primary); font-size: 12px; width: 240px; height: 38px; box-sizing: border-box; outline: none; }
.filter-select { background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 0 12px; color: var(--text-primary); font-size: 12px; height: 38px; outline: none; }

.table-container { overflow: visible; }
.orders-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.orders-table th { text-align: left; padding: 12px 14px; color: var(--text-secondary); font-size: 12px; font-weight: 600; border-bottom: 1px solid var(--border-color); }
.orders-table td { padding: 14px; border-bottom: 1px solid var(--border-color); vertical-align: middle; }
.order-id { color: #E67E22; font-size: 13px; display: block; }
.sub-text { display: block; font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.plate { font-weight: 700; color: var(--text-primary); }
.service-text { display: block; font-size: 13px; margin-bottom: 5px; }
.mini-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.mini-tag { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; padding: 2px 8px; border-radius: 6px; background: var(--input-bg); color: var(--text-secondary); }
.mini-tag.certified { background: rgba(46, 204, 113, 0.14); color: #2ECC71; }

/* Selector desplegable de mecánicos con barra de carga */
.mech-cell { position: relative; min-width: 220px; }
.mech-dropdown { position: relative; }
.mech-trigger { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 10px; background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 8px 10px; color: var(--text-primary); cursor: pointer; text-align: left; }
.mech-trigger:hover { border-color: #E67E22; }
.mech-info { flex: 1; }
.mech-info strong { display: block; font-size: 12px; margin-bottom: 4px; }
.load-ratio { font-size: 11px; font-weight: 700; color: var(--text-secondary); }
.load-bar { width: 100%; height: 5px; background: rgba(255, 255, 255, 0.1); border-radius: 999px; overflow: hidden; }
.load-bar.full { margin-top: 6px; height: 6px; }
.load-fill { height: 100%; border-radius: 999px; transition: width 0.25s ease; }

.mech-menu { position: absolute; top: calc(100% + 6px); left: 0; width: 270px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 12px; padding: 8px; z-index: 30; box-shadow: 0 14px 28px rgba(0, 0, 0, 0.45); display: flex; flex-direction: column; gap: 6px; }
.mech-menu-header { font-size: 11px; font-weight: 600; color: var(--text-secondary); padding: 4px 6px; }
.mech-option { background: var(--input-bg); border: 1px solid transparent; border-radius: 8px; padding: 8px 10px; text-align: left; color: var(--text-primary); cursor: pointer; }
.mech-option:hover, .mech-option.selected { border-color: #E67E22; }
.opt-top { display: flex; justify-content: space-between; align-items: center; font-size: 12px; }
.opt-badge { font-size: 11px; font-weight: 600; }
.mech-option small { display: block; font-size: 11px; color: var(--text-secondary); margin-top: 2px; }

.status-badge { padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 600; white-space: nowrap; }
.status-badge.pendiente { background: rgba(241, 196, 15, 0.15); color: #F1C40F; }
.status-badge.en-proceso { background: rgba(52, 152, 219, 0.15); color: #3498DB; }
.status-badge.control-de-calidad { background: rgba(46, 204, 113, 0.15); color: #2ECC71; }

.actions-col { text-align: right; white-space: nowrap; }
.action-btn { background: var(--input-bg); border: 1px solid var(--border-color); color: var(--text-primary); padding: 7px 11px; border-radius: 8px; font-size: 12px; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; margin-left: 6px; transition: 0.2s; }
.action-btn:hover { border-color: #E67E22; color: #E67E22; }
.action-btn.orange { background: rgba(230, 126, 34, 0.14); border-color: rgba(230, 126, 34, 0.4); color: #E67E22; font-weight: 600; }
</style>