<template>
  <AppShell
    :isDarkMode="isDarkMode"
    :navItems="navItems"
    activeKey="ordenes"
    title="Gestión de Órdenes de Servicio"
    subtitle="Asignación de mecánicos por carga, control técnico de garantía, repuestos y liquidación total"
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
            <option value="DIAGNOSTICO">DIAGNOSTICO</option>
            <option value="PENDIENTE">PENDIENTE</option>
            <option value="EN_PROCESO">EN_PROCESO</option>
            <option value="FINALIZADA">FINALIZADA</option>
            <option value="ENTREGADA">ENTREGADA</option>
            <option value="CANCELADA">CANCELADA</option>
          </select>
        </div>
      </div>

      <div v-if="mensajeFeedback" class="feedback-banner" :class="tipoFeedback">
        {{ mensajeFeedback }}
      </div>

      <div class="table-container">
        <table class="orders-table">
          <thead>
            <tr>
              <th>N° Orden</th>
              <th>Cliente y Vehículo</th>
              <th>Servicio, Garantía y Total</th>
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
                <small v-if="orden.ordenPadreGarantiaId" class="warranty-ref">
                  Reingreso Gar.: {{ orden.ordenPadreGarantiaId }}
                </small>
              </td>

              <td>
                <strong>{{ orden.cliente }}</strong>
                <small class="sub-text">{{ orden.vehiculo }} · <span class="plate">{{ orden.placa }}</span></small>
              </td>

              <td>
                <span class="service-text">{{ orden.servicio }}</span>
                <div class="mini-tags">
                  <span class="mini-tag" :class="{ certified: orden.garantia?.certificada || orden.garantia?.estadoGarantia === 'VIGENTE' || orden.garantia?.estadoGarantia === 'RECLAMADA' }">
                    <ShieldCheck :size="12" />
                    {{ etiquetaGarantia(orden) }}
                  </span>
                  <span class="mini-tag">
                    <Wrench :size="12" /> {{ (orden.solicitudesPiezas || []).length }} pieza(s)
                  </span>
                  <span class="mini-tag total-pill">
                    Total: {{ formatCOP(calcularTotalOrden(orden)) }}
                  </span>
                </div>
              </td>

              <!-- Desplegable interactivo con barra de carga de mecánicos -->
              <td class="mech-cell">
                <div class="mech-dropdown">
                  <button
                    type="button"
                    class="mech-trigger"
                    :disabled="esEstadoTerminal(orden.estado)"
                    @click="toggleDropdown(orden.id)"
                  >
                    <div class="mech-info">
                      <strong>{{ getMecanico(orden.mecanicoId)?.nombre || 'Sin asignar' }}</strong>
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
                      {{ getMecanico(orden.mecanicoId)?.ordenesActivas ?? 0 }}/{{ getMecanico(orden.mecanicoId)?.capacidadMaxima ?? 5 }}
                    </span>
                    <ChevronDown :size="15" />
                  </button>

                  <div v-if="dropdownAbierto === orden.id && !esEstadoTerminal(orden.estado)" class="mech-menu">
                    <div class="mech-menu-header">Seleccionar mecánico según carga</div>
                    <button
                      v-for="mec in mecanicos"
                      :key="mec.id"
                      type="button"
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
                <small v-if="orden.motivoCancelacion" class="sub-text">Motivo: {{ orden.motivoCancelacion }}</small>
              </td>

              <td class="actions-col">
                <button
                  type="button"
                  class="action-btn"
                  :class="{ active: ordenSeleccionada?.id === orden.id }"
                  @click="seleccionarOrdenLiquidacion(orden)"
                  title="Mano de Obra, Cobertura de Garantía y Total"
                >
                  <Calculator :size="15" /> Liquidación
                </button>
                <button
                  type="button"
                  class="action-btn"
                  :disabled="esEstadoTerminal(orden.estado)"
                  @click="ordenGarantia = orden"
                  title="Checklist Técnico y Garantía"
                >
                  <ClipboardCheck :size="15" /> Garantía
                </button>
                <button
                  type="button"
                  class="action-btn orange"
                  @click="ordenPiezas = orden"
                  title="Solicitar o Aprobar Piezas"
                >
                  <Cog :size="15" /> Piezas
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Sección S2-T08: Mano de Obra, Cobertura de Garantía y Total en la orden -->
    <section v-if="ordenLiquidacionActiva" class="card liquidation-card">
      <div class="liq-header">
        <div>
          <span class="section-tag">Liquidación de Orden · S2-T08</span>
          <h3>Mano de Obra, Cobertura de Garantía y Total — {{ ordenLiquidacionActiva.id }}</h3>
          <small class="sub-text">
            {{ ordenLiquidacionActiva.cliente }} · {{ ordenLiquidacionActiva.vehiculo }} ({{ ordenLiquidacionActiva.placa }}) · Estado: <strong>{{ ordenLiquidacionActiva.estado }}</strong>
          </small>
        </div>
        <button type="button" class="btn-ghost" @click="ordenSeleccionadaId = null">Ocultar detalle</button>
      </div>

      <div class="liq-grid">
        <!-- Bloque 1: Mano de Obra -->
        <div class="liq-box">
          <div class="liq-box-head">
            <Wrench :size="16" class="ico-amber" />
            <h4>Mano de Obra</h4>
          </div>
          <p class="liq-desc">Valor del servicio técnico asignado a la orden de trabajo.</p>

          <form class="labor-form" @submit.prevent="guardarManoObra">
            <label for="input-mano-obra">Costo de mano de obra (COP)</label>
            <div class="labor-row">
              <input
                id="input-mano-obra"
                type="number"
                min="0"
                step="1000"
                v-model.number="borradorManoObra"
                :disabled="esEstadoTerminal(ordenLiquidacionActiva.estado) || guardandoManoObra"
                required
              />
              <button
                type="submit"
                class="btn-primary"
                :disabled="esEstadoTerminal(ordenLiquidacionActiva.estado) || guardandoManoObra"
              >
                Guardar
              </button>
            </div>
          </form>

          <div class="liq-line">
            <span>Mano de obra facturable</span>
            <strong>{{ formatCOP(manoObraFacturable(ordenLiquidacionActiva)) }}</strong>
          </div>
        </div>

        <!-- Bloque 2: Cobertura de Garantía -->
        <div class="liq-box">
          <div class="liq-box-head">
            <ShieldCheck :size="16" class="ico-blue" />
            <h4>Cobertura de Garantía</h4>
          </div>
          <p class="liq-desc">Estado del certificado, vigencia y exención de ítems cubiertos.</p>

          <div class="cov-status-row">
            <span>Estado de garantía</span>
            <span class="cov-badge" :class="slugEstado(ordenLiquidacionActiva.garantia?.estadoGarantia || 'PENDIENTE')">
              {{ ordenLiquidacionActiva.garantia?.estadoGarantia || (ordenLiquidacionActiva.garantia?.certificada ? 'VIGENTE' : 'PENDIENTE') }}
            </span>
          </div>

          <div class="liq-line">
            <span>Vigencia configurada</span>
            <strong>
              {{ ordenLiquidacionActiva.garantia?.dias ?? 30 }} días / {{ ordenLiquidacionActiva.garantia?.kilometros ?? 1000 }} km
            </strong>
          </div>

          <div class="liq-line">
            <span>Piezas exentas por garantía</span>
            <strong class="text-green">{{ formatCOP(subtotalPiezasExentas(ordenLiquidacionActiva)) }}</strong>
          </div>

          <div class="liq-line">
            <span>Tipo de ingreso</span>
            <strong>{{ ordenLiquidacionActiva.ordenPadreGarantiaId ? `Reingreso (${ordenLiquidacionActiva.ordenPadreGarantiaId})` : 'Orden estándar' }}</strong>
          </div>
        </div>

        <!-- Bloque 3: Total de la Orden -->
        <div class="liq-box total-box">
          <div class="liq-box-head">
            <Calculator :size="16" class="ico-green" />
            <h4>Total de la Orden</h4>
          </div>
          <p class="liq-desc">Mano de obra + piezas aprobadas a precio histórico (excluyendo garantía).</p>

          <div class="liq-line">
            <span>Mano de obra</span>
            <span>{{ formatCOP(manoObraFacturable(ordenLiquidacionActiva)) }}</span>
          </div>

          <div class="liq-line">
            <span>Piezas aprobadas facturables</span>
            <span>{{ formatCOP(subtotalPiezasFacturables(ordenLiquidacionActiva)) }}</span>
          </div>

          <div class="liq-line">
            <span>Descuento por garantía</span>
            <span class="text-green">-{{ formatCOP(descuentoTotalGarantia(ordenLiquidacionActiva)) }}</span>
          </div>

          <div class="total-highlight">
            <span>Total a liquidar</span>
            <strong>{{ formatCOP(calcularTotalOrden(ordenLiquidacionActiva)) }}</strong>
          </div>
        </div>
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
import { ref, computed, onMounted, watch } from 'vue'
import {
  LayoutDashboard, Package, Users, ClipboardList,
  Search, ChevronDown, ShieldCheck, Wrench, ClipboardCheck, Cog, Calculator
} from 'lucide-vue-next'
import AppShell from '../components/AppShell.vue'
import GarantiaChecklistModal from '../components/GarantiaChecklistModal.vue'
import SolicitudPiezasModal from '../components/SolicitudPiezasModal.vue'
import { ordenesService, calcularTotalOrden } from '../services/ordenesService'

defineProps({ isDarkMode: Boolean })
defineEmits(['change-view', 'toggle-theme'])

const navItems = [
  { key: 'gerente', label: 'Gerencia', icon: LayoutDashboard },
  { key: 'ordenes', label: 'Órdenes', icon: ClipboardList },
  { key: 'inventario', label: 'Inventario', icon: Package },
  { key: 'trabajadores', label: 'Trabajadores', icon: Users }
]

const ordenes = ref([])
const mecanicos = ref([])
const busqueda = ref('')
const filtroEstado = ref('')
const dropdownAbierto = ref(null)
const ordenGarantia = ref(null)
const ordenPiezas = ref(null)
const ordenSeleccionadaId = ref(null)
const borradorManoObra = ref(0)
const guardandoManoObra = ref(false)
const mensajeFeedback = ref('')
const tipoFeedback = ref('ok')

onMounted(async () => {
  ordenes.value = await ordenesService.obtenerOrdenes()
  mecanicos.value = await ordenesService.obtenerMecanicos()
  if (ordenes.value.length > 0) {
    ordenSeleccionadaId.value = ordenes.value[0].id
    borradorManoObra.value = Number(ordenes.value[0].costoManoObra) || 0
  }
})

const ordenSeleccionada = computed(() =>
  ordenes.value.find((o) => o.id === ordenSeleccionadaId.value) || null
)

const ordenLiquidacionActiva = computed(() => ordenSeleccionada.value)

watch(
  ordenSeleccionada,
  (nuevaOrden) => {
    if (nuevaOrden) {
      borradorManoObra.value = Number(nuevaOrden.costoManoObra) || 0
    }
  },
  { immediate: true }
)

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

const esEstadoTerminal = (estado) => estado === 'ENTREGADA' || estado === 'CANCELADA'

const getMecanico = (id) => mecanicos.value.find(m => m.id === id)
const getPorcentajeCarga = (mec) => mec ? Math.round((mec.ordenesActivas / mec.capacidadMaxima) * 100) : 0
const getColorCarga = (mec) => {
  const pct = getPorcentajeCarga(mec)
  if (pct >= 80) return '#E74C3C'
  if (pct >= 50) return '#F39C12'
  return '#2ECC71'
}

const toggleDropdown = (ordenId) => {
  const orden = ordenes.value.find((o) => o.id === ordenId)
  if (orden && esEstadoTerminal(orden.estado)) return
  dropdownAbierto.value = dropdownAbierto.value === ordenId ? null : ordenId
}

const asignarMecanico = (orden, nuevoMecanicoId) => {
  if (esEstadoTerminal(orden.estado)) return
  if (orden.mecanicoId !== nuevoMecanicoId) {
    const anterior = getMecanico(orden.mecanicoId)
    const nuevo = getMecanico(nuevoMecanicoId)
    if (anterior && anterior.ordenesActivas > 0) anterior.ordenesActivas--
    if (nuevo) nuevo.ordenesActivas++
    orden.mecanicoId = nuevoMecanicoId
  }
  dropdownAbierto.value = null
}

const seleccionarOrdenLiquidacion = (orden) => {
  ordenSeleccionadaId.value = orden.id
  borradorManoObra.value = Number(orden.costoManoObra) || 0
  mensajeFeedback.value = ''
}

const guardarManoObra = async () => {
  if (!ordenLiquidacionActiva.value) return
  guardandoManoObra.value = true
  mensajeFeedback.value = ''
  try {
    const res = await ordenesService.actualizarManoObraOrden(
      ordenLiquidacionActiva.value.id,
      borradorManoObra.value
    )
    const target = ordenes.value.find((o) => o.id === ordenLiquidacionActiva.value.id)
    if (target) {
      target.costoManoObra = res.costoManoObra
      target.totalOrden = res.totalOrden
    }
    tipoFeedback.value = 'ok'
    mensajeFeedback.value = `Mano de obra actualizada para ${res.ordenId}. Total: ${formatCOP(res.totalOrden)}.`
  } catch (err) {
    tipoFeedback.value = 'error'
    mensajeFeedback.value = err?.message || 'No se pudo actualizar la mano de obra.'
  } finally {
    guardandoManoObra.value = false
  }
}

const actualizarGarantia = async ({ ordenId, garantia }) => {
  const resultado = await ordenesService.actualizarGarantiaOrden(ordenId, garantia, garantia.certificada)
  const target = ordenes.value.find(o => o.id === ordenId)
  if (target) {
    target.garantia = resultado.garantia
    if (resultado.nuevoEstado) {
      target.estado = resultado.nuevoEstado
    }
    target.totalOrden = calcularTotalOrden(target)
  }
  ordenGarantia.value = null
}

const actualizarPiezas = async ({ ordenId, solicitudes, totalOrden }) => {
  const target = ordenes.value.find(o => o.id === ordenId)
  if (target) {
    target.solicitudesPiezas = solicitudes
    target.totalOrden = typeof totalOrden === 'number' ? totalOrden : calcularTotalOrden(target)
  }
  mecanicos.value = await ordenesService.obtenerMecanicos()
}

const ordenCubiertaCompleta = (orden) =>
  Boolean(orden?.ordenPadreGarantiaId) && orden?.garantia?.estadoGarantia === 'RECLAMADA'

const manoObraFacturable = (orden) => {
  if (!orden) return 0
  if (ordenCubiertaCompleta(orden)) return 0
  return Math.max(0, Number(orden.costoManoObra) || 0)
}

const subtotalPiezasFacturables = (orden) =>
  (orden?.solicitudesPiezas || []).reduce((acc, item) => {
    const cubierta = Boolean(item.cubiertoPorGarantia) || ordenCubiertaCompleta(orden)
    if (item.estado !== 'APROBADA' || cubierta) return acc
    const precio = Number(item.precioUnitarioHistorico ?? item.precioVenta ?? 0)
    return acc + precio * (Number(item.cantidad) || 0)
  }, 0)

const subtotalPiezasExentas = (orden) =>
  (orden?.solicitudesPiezas || []).reduce((acc, item) => {
    const cubierta = Boolean(item.cubiertoPorGarantia) || ordenCubiertaCompleta(orden)
    if (item.estado !== 'APROBADA' || !cubierta) return acc
    const precio = Number(item.precioUnitarioHistorico ?? item.precioVenta ?? 0)
    return acc + precio * (Number(item.cantidad) || 0)
  }, 0)

const descuentoTotalGarantia = (orden) => {
  const manoObraBruta = Math.max(0, Number(orden?.costoManoObra) || 0)
  const descManoObra = ordenCubiertaCompleta(orden) ? manoObraBruta : 0
  return descManoObra + subtotalPiezasExentas(orden)
}

const etiquetaGarantia = (orden) => {
  const estado = orden?.garantia?.estadoGarantia
  if (estado === 'RECLAMADA') return 'Garantía reclamada'
  if (estado === 'VIGENTE' || orden?.garantia?.certificada) {
    return `Garantía ${orden.garantia?.dias ?? 30}d / ${orden.garantia?.kilometros ?? 1000}km`
  }
  return 'Garantía pendiente'
}

const formatCOP = (valor) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0
  }).format(Number(valor) || 0)

const slugEstado = (estado) => (estado || '').toLowerCase().replace(/[\s_]+/g, '-')
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

.feedback-banner { padding: 10px 14px; border-radius: 10px; font-size: 12px; font-weight: 600; }
.feedback-banner.ok { background: rgba(46, 204, 113, 0.14); border: 1px solid rgba(46, 204, 113, 0.4); color: #2ECC71; }
.feedback-banner.error { background: rgba(231, 76, 60, 0.14); border: 1px solid rgba(231, 76, 60, 0.4); color: #E74C3C; }

.table-container { overflow-x: auto; }
.orders-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.orders-table th { text-align: left; padding: 12px 14px; color: var(--text-secondary); font-size: 12px; font-weight: 600; border-bottom: 1px solid var(--border-color); }
.orders-table td { padding: 14px; border-bottom: 1px solid var(--border-color); vertical-align: middle; }
.order-id { color: #E67E22; font-size: 13px; display: block; }
.sub-text { display: block; font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.warranty-ref { display: inline-block; margin-top: 4px; font-size: 10px; font-weight: 600; color: #3498DB; }
.plate { font-weight: 700; color: var(--text-primary); }
.service-text { display: block; font-size: 13px; margin-bottom: 6px; }
.mini-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.mini-tag { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; padding: 2px 8px; border-radius: 6px; background: var(--input-bg); color: var(--text-secondary); }
.mini-tag.certified { background: rgba(46, 204, 113, 0.14); color: #2ECC71; }
.mini-tag.total-pill { background: rgba(230, 126, 34, 0.14); color: #E67E22; font-weight: 700; }

/* Selector desplegable de mecánicos con barra de carga */
.mech-cell { position: relative; min-width: 220px; }
.mech-dropdown { position: relative; }
.mech-trigger { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 10px; background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 8px 10px; color: var(--text-primary); cursor: pointer; text-align: left; }
.mech-trigger:hover:not(:disabled) { border-color: #E67E22; }
.mech-trigger:disabled { opacity: 0.55; cursor: default; }
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
.status-badge.diagnostico { background: rgba(155, 89, 182, 0.15); color: #9B59B6; }
.status-badge.pendiente { background: rgba(241, 196, 15, 0.15); color: #F1C40F; }
.status-badge.en-proceso { background: rgba(52, 152, 219, 0.15); color: #3498DB; }
.status-badge.finalizada, .status-badge.entregada { background: rgba(46, 204, 113, 0.15); color: #2ECC71; }
.status-badge.cancelada { background: rgba(231, 76, 60, 0.15); color: #E74C3C; }

.actions-col { text-align: right; white-space: nowrap; }
.action-btn { background: var(--input-bg); border: 1px solid var(--border-color); color: var(--text-primary); padding: 7px 10px; border-radius: 8px; font-size: 12px; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; margin-left: 6px; transition: 0.2s; }
.action-btn:hover:not(:disabled), .action-btn.active { border-color: #E67E22; color: #E67E22; }
.action-btn:disabled { opacity: 0.45; cursor: default; }
.action-btn.orange { background: rgba(230, 126, 34, 0.14); border-color: rgba(230, 126, 34, 0.4); color: #E67E22; font-weight: 600; }

/* Sección Mano de Obra, Cobertura de Garantía y Total */
.liquidation-card { gap: 18px; }
.liq-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.section-tag { font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #E67E22; font-weight: 700; }
.liq-header h3 { margin: 4px 0 0; font-size: 16px; }
.btn-ghost { background: transparent; border: 1px solid var(--border-color); color: var(--text-secondary); padding: 7px 12px; border-radius: 8px; font-size: 12px; cursor: pointer; }
.btn-ghost:hover { color: var(--text-primary); border-color: #E67E22; }

.liq-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.liq-box { background: var(--input-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.total-box { border-color: rgba(230, 126, 34, 0.45); }
.liq-box-head { display: flex; align-items: center; gap: 8px; }
.liq-box-head h4 { margin: 0; font-size: 14px; font-weight: 600; }
.ico-amber { color: #E67E22; }
.ico-blue { color: #3498DB; }
.ico-green { color: #2ECC71; }
.liq-desc { margin: 0; font-size: 12px; color: var(--text-secondary); }

.labor-form { display: flex; flex-direction: column; gap: 6px; margin-bottom: 4px; }
.labor-form label { font-size: 11px; color: var(--text-secondary); font-weight: 600; }
.labor-row { display: flex; gap: 8px; }
.labor-row input { flex: 1; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 8px 10px; color: var(--text-primary); font-size: 13px; height: 36px; box-sizing: border-box; outline: none; }
.labor-row input:focus { border-color: #E67E22; }
.btn-primary { background: #E67E22; border: none; color: #fff; padding: 0 14px; height: 36px; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; }
.btn-primary:disabled { opacity: 0.45; cursor: default; }

.cov-status-row { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-secondary); }
.cov-badge { padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 700; background: var(--bg-card); color: var(--text-primary); }
.cov-badge.vigente, .cov-badge.reclamada { background: rgba(46, 204, 113, 0.16); color: #2ECC71; }
.cov-badge.vencida, .cov-badge.anulada-por-manipulacion, .cov-badge.anulada { background: rgba(231, 76, 60, 0.16); color: #E74C3C; }

.liq-line { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-secondary); padding-top: 6px; border-top: 1px solid var(--border-color); }
.liq-line strong { color: var(--text-primary); }
.text-green { color: #2ECC71 !important; font-weight: 600; }

.total-highlight { margin-top: auto; padding-top: 10px; border-top: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.total-highlight span { font-size: 13px; font-weight: 600; }
.total-highlight strong { font-size: 18px; color: #E67E22; }

@media (max-width: 960px) {
  .liq-grid { grid-template-columns: 1fr; }
}
</style>