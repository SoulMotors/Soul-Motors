export const mecanicosMock = [
  { id: 1, nombre: 'Carlos Pérez', especialidad: 'Motor y Transmisión', ordenesActivas: 4, capacidadMaxima: 5 },
  { id: 2, nombre: 'Luis Martínez', especialidad: 'Frenos y Suspensión', ordenesActivas: 2, capacidadMaxima: 5 },
  { id: 3, nombre: 'Diana Torres', especialidad: 'Sistema Eléctrico', ordenesActivas: 1, capacidadMaxima: 5 },
  { id: 4, nombre: 'Andrés Silva', especialidad: 'Mantenimiento General', ordenesActivas: 5, capacidadMaxima: 5 }
]

export const repuestosDisponiblesMock = [
  { id: 101, sku: 'REP-01', nombre: 'Kit de Arrastre Yamaha MT-03', stock: 8, precioVenta: 245000 },
  { id: 102, sku: 'REP-02', nombre: 'Pastillas de Freno Delanteras', stock: 14, precioVenta: 85000 },
  { id: 103, sku: 'REP-03', nombre: 'Batería Gel 12V 7Ah', stock: 3, precioVenta: 160000 },
  { id: 104, sku: 'REP-04', nombre: 'Aceite Sintético 10W-40 (1L)', stock: 22, precioVenta: 52000 }
]

export const ordenesInicialesMock = [
  {
    id: 'ORD-001',
    cliente: 'Mateo Restrepo',
    vehiculo: 'Yamaha MT-03',
    placa: 'XYZ-12A',
    servicio: 'Mantenimiento general y sincronización',
    mecanicoId: 1,
    ordenPadreGarantiaId: null,
    estado: 'EN_PROCESO',
    motivoCancelacion: null,
    fechaIngreso: '2026-11-23',
    costoManoObra: 120000,
    totalOrden: 224000,
    garantia: {
      estadoGarantia: 'PENDIENTE',
      checklist: {
        torque: true,
        frenos: true,
        sistemaElectrico: false,
        pruebaRuta: false,
        estanqueidadEmpaques: false
      },
      observaciones: 'Pendiente revisión de sensor de oxígeno y prueba final en vía.',
      dias: 30,
      kilometros: 1000,
      kmEntrega: null,
      seriesLotesPiezas: '',
      certificada: false
    },
    solicitudesPiezas: [
      {
        id: 1,
        piezaId: 104,
        sku: 'REP-04',
        repuesto: 'Aceite Sintético 10W-40 (1L)',
        cantidad: 2,
        precioUnitarioHistorico: 52000,
        cubiertoPorGarantia: false,
        stockActual: 22,
        estado: 'APROBADA',
        nota: 'Cambio programado'
      },
      {
        id: 2,
        piezaId: 101,
        sku: 'REP-01',
        repuesto: 'Kit de Arrastre Yamaha MT-03',
        cantidad: 1,
        precioUnitarioHistorico: 245000,
        cubiertoPorGarantia: false,
        stockActual: 8,
        estado: 'SOLICITADA',
        nota: 'Desgaste excesivo en piñón'
      }
    ]
  },
  {
    id: 'ORD-002',
    cliente: 'Sofía Mendoza',
    vehiculo: 'KTM Duke 200',
    placa: 'KLM-89F',
    servicio: 'Cambio de pastillas y purga de frenos ABS',
    mecanicoId: 2,
    ordenPadreGarantiaId: null,
    estado: 'FINALIZADA',
    motivoCancelacion: null,
    fechaIngreso: '2026-11-22',
    costoManoObra: 65000,
    totalOrden: 150000,
    garantia: {
      estadoGarantia: 'VIGENTE',
      checklist: {
        torque: true,
        frenos: true,
        sistemaElectrico: true,
        pruebaRuta: true,
        estanqueidadEmpaques: true
      },
      observaciones: 'Frenado óptimo en prueba de ruta.',
      dias: 45,
      kilometros: 1500,
      kmEntrega: 12400,
      seriesLotesPiezas: 'LOTE-FR-08',
      certificada: true
    },
    solicitudesPiezas: [
      {
        id: 3,
        piezaId: 102,
        sku: 'REP-02',
        repuesto: 'Pastillas de Freno Delanteras',
        cantidad: 1,
        precioUnitarioHistorico: 85000,
        cubiertoPorGarantia: false,
        stockActual: 14,
        estado: 'APROBADA',
        nota: 'Reemplazo urgente'
      }
    ]
  },
  {
    id: 'ORD-003',
    cliente: 'Julián Castro',
    vehiculo: 'Suzuki Gixxer 250',
    placa: 'PQR-45C',
    servicio: 'Reingreso por garantía: revisión de carga y batería',
    mecanicoId: 3,
    ordenPadreGarantiaId: 'ORD-000',
    estado: 'EN_PROCESO',
    motivoCancelacion: null,
    fechaIngreso: '2026-11-23',
    costoManoObra: 0,
    totalOrden: 0,
    garantia: {
      estadoGarantia: 'RECLAMADA',
      checklist: {
        torque: false,
        frenos: false,
        sistemaElectrico: false,
        pruebaRuta: false,
        estanqueidadEmpaques: false
      },
      observaciones: 'Cubierto por garantía vigente de intervención eléctrica previa.',
      dias: 30,
      kilometros: 1000,
      kmEntrega: null,
      seriesLotesPiezas: '',
      certificada: false
    },
    solicitudesPiezas: [
      {
        id: 4,
        piezaId: 103,
        sku: 'REP-03',
        repuesto: 'Batería Gel 12V 7Ah',
        cantidad: 1,
        precioUnitarioHistorico: 160000,
        cubiertoPorGarantia: true,
        stockActual: 3,
        estado: 'SOLICITADA',
        nota: 'Voltaje por debajo de 11.5V (exento por garantía)'
      }
    ]
  }
]