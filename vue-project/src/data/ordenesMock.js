export const mecanicosMock = [
  { id: 1, nombre: 'Carlos Pérez', especialidad: 'Motor y Transmisión', ordenesActivas: 4, capacidadMaxima: 5 },
  { id: 2, nombre: 'Luis Martínez', especialidad: 'Frenos y Suspensión', ordenesActivas: 2, capacidadMaxima: 5 },
  { id: 3, nombre: 'Diana Torres', especialidad: 'Sistema Eléctrico', ordenesActivas: 1, capacidadMaxima: 5 },
  { id: 4, nombre: 'Andrés Silva', especialidad: 'Mantenimiento General', ordenesActivas: 5, capacidadMaxima: 5 }
]

export const repuestosDisponiblesMock = [
  { id: 101, codigo: 'REP-01', nombre: 'Kit de Arrastre Yamaha MT-03', stock: 8, precio: 245000 },
  { id: 102, codigo: 'REP-02', nombre: 'Pastillas de Freno Delanteras', stock: 14, precio: 85000 },
  { id: 103, codigo: 'REP-03', nombre: 'Batería Gel 12V 7Ah', stock: 3, precio: 160000 },
  { id: 104, codigo: 'REP-04', nombre: 'Aceite Sintético 10W-40 (1L)', stock: 22, precio: 52000 }
]

export const ordenesInicialesMock = [
  {
    id: 'ORD-001',
    cliente: 'Mateo Restrepo',
    vehiculo: 'Yamaha MT-03',
    placa: 'XYZ-12A',
    servicio: 'Mantenimiento general y sincronización',
    mecanicoId: 1,
    estado: 'En Proceso',
    fechaIngreso: '2026-11-23',
    garantia: {
      checklist: {
        torque: true,
        frenos: true,
        sistemaElectrico: false,
        pruebaRuta: false
      },
      observaciones: 'Pendiente revisión de sensor de oxígeno y prueba final en vía.',
      dias: 30,
      kilometros: 1000,
      certificada: false
    },
    solicitudesPiezas: [
      { id: 1, repuesto: 'Aceite Sintético 10W-40 (1L)', cantidad: 2, stockActual: 22, estado: 'Aprobada', nota: 'Cambio programado' },
      { id: 2, repuesto: 'Kit de Arrastre Yamaha MT-03', cantidad: 1, stockActual: 8, estado: 'Pendiente', nota: 'Desgaste excesivo en piñón' }
    ]
  },
  {
    id: 'ORD-002',
    cliente: 'Sofía Mendoza',
    vehiculo: 'KTM Duke 200',
    placa: 'KLM-89F',
    servicio: 'Cambio de pastillas y purga de frenos ABS',
    mecanicoId: 2,
    estado: 'Control de Calidad',
    fechaIngreso: '2026-11-22',
    garantia: {
      checklist: {
        torque: true,
        frenos: true,
        sistemaElectrico: true,
        pruebaRuta: true
      },
      observaciones: 'Frenado óptimo en prueba de ruta.',
      dias: 45,
      kilometros: 1500,
      certificada: true
    },
    solicitudesPiezas: [
      { id: 3, repuesto: 'Pastillas de Freno Delanteras', cantidad: 1, stockActual: 14, estado: 'Aprobada', nota: 'Reemplazo urgente' }
    ]
  },
  {
    id: 'ORD-003',
    cliente: 'Julián Castro',
    vehiculo: 'Suzuki Gixxer 250',
    placa: 'PQR-45C',
    servicio: 'Diagnóstico falla de encendido y batería',
    mecanicoId: 3,
    estado: 'Pendiente',
    fechaIngreso: '2026-11-23',
    garantia: {
      checklist: {
        torque: false,
        frenos: false,
        sistemaElectrico: false,
        pruebaRuta: false
      },
      observaciones: '',
      dias: 30,
      kilometros: 1000,
      certificada: false
    },
    solicitudesPiezas: [
      { id: 4, repuesto: 'Batería Gel 12V 7Ah', cantidad: 1, stockActual: 3, estado: 'Pendiente', nota: 'Voltaje por debajo de 11.5V' }
    ]
  }
]