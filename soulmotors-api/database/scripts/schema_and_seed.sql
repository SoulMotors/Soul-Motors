-- ==========================================
-- SCRIPT DDL Y SEED v2.1 - SOUL MOTORS
-- Entorno: Neon (PostgreSQL Cloud)
-- Sprint 1: Tablas base, Proveedores, Auditoría y Triggers
-- ==========================================

-- 0. Función Genérica para actualizado_at
CREATE OR REPLACE FUNCTION actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. Tabla de Auditoría
CREATE TABLE registro_auditoria (
    id BIGSERIAL PRIMARY KEY,
    tabla_afectada VARCHAR(50) NOT NULL,
    accion VARCHAR(20) NOT NULL,
    usuario_id BIGINT,
    detalle JSONB,
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER trg_auditoria_actualizado_at BEFORE UPDATE ON registro_auditoria FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- 2. Tabla de Usuarios
CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    rol VARCHAR(50) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'GERENTE', 'MECANICO', 'VENDEDOR', 'EMPLEADO')),
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER trg_usuario_actualizado_at BEFORE UPDATE ON usuario FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- 3. Tabla de Clientes
CREATE TABLE cliente (
    id BIGSERIAL PRIMARY KEY,
    documento VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER trg_cliente_actualizado_at BEFORE UPDATE ON cliente FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- 4. Tabla de Vehículos (Relación 1:N con Cliente)
CREATE TABLE vehiculo (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    placa VARCHAR(20) UNIQUE NOT NULL,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50) NOT NULL,
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE RESTRICT
);
CREATE TRIGGER trg_vehiculo_actualizado_at BEFORE UPDATE ON vehiculo FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- 5. Tabla de Proveedores
CREATE TABLE proveedor (
    id BIGSERIAL PRIMARY KEY,
    nit VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    email VARCHAR(100),
    direccion VARCHAR(150),
    tiempo_entrega_dias INT NOT NULL CHECK (tiempo_entrega_dias >= 0), -- 0 = entrega el mismo día
    activo BOOLEAN DEFAULT TRUE,
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER trg_proveedor_actualizado_at BEFORE UPDATE ON proveedor FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- 6. Tabla de Piezas (Catálogo e Inventario)
CREATE TABLE pieza (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    categoria VARCHAR(50) NOT NULL CHECK (categoria IN ('Repuesto', 'Accesorio')),
    precio_venta DECIMAL(10,2) NOT NULL CHECK (precio_venta >= 0),
    stock_actual INT NOT NULL CHECK (stock_actual >= 0),
    stock_minimo INT NOT NULL CHECK (stock_minimo >= 0),
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER trg_pieza_actualizado_at BEFORE UPDATE ON pieza FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- 7. Tabla Intermedia Pieza_Proveedor
CREATE TABLE pieza_proveedor (
    id BIGSERIAL PRIMARY KEY,
    pieza_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    prioridad INT NOT NULL CHECK (prioridad >= 1),
    costo_pactado DECIMAL(10,2) NOT NULL CHECK (costo_pactado >= 0),
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_pp_pieza FOREIGN KEY (pieza_id) REFERENCES pieza(id) ON DELETE CASCADE,
    CONSTRAINT fk_pp_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id) ON DELETE RESTRICT,
    CONSTRAINT uq_pieza_proveedor UNIQUE (pieza_id, proveedor_id),
    -- Diferida al COMMIT para permitir intercambiar prioridades entre proveedores sin choque
    CONSTRAINT uq_pieza_prioridad UNIQUE (pieza_id, prioridad) DEFERRABLE INITIALLY DEFERRED
);
CREATE TRIGGER trg_pieza_proveedor_actualizado_at BEFORE UPDATE ON pieza_proveedor FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- 8. Tabla Movimiento_Inventario
CREATE TABLE movimiento_inventario (
    id BIGSERIAL PRIMARY KEY,
    pieza_id BIGINT NOT NULL,
    tipo_movimiento VARCHAR(20) NOT NULL CHECK (tipo_movimiento IN ('Entrada', 'Salida')),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mov_pieza FOREIGN KEY (pieza_id) REFERENCES pieza(id) ON DELETE RESTRICT
);
CREATE TRIGGER trg_mov_inv_actualizado_at BEFORE UPDATE ON movimiento_inventario FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- ==========================================
-- DATOS SEMILLA (DINÁMICOS SIN IDs QUEMADOS)
-- ==========================================

-- Hash BCrypt de prueba para entorno dev (contraseña: "SoulMotors2026*")
INSERT INTO usuario (nombre, rol, username, password) VALUES
('Admin General', 'ADMINISTRADOR', 'admin', '$2a$10$3aPn0nxkbPeaqrpa2Nqcyuox34MrI7sobtj4H.l6Bqopi5KD7GyJe'),
('Mecánico Juan', 'MECANICO', 'empleado1', '$2a$10$3aPn0nxkbPeaqrpa2Nqcyuox34MrI7sobtj4H.l6Bqopi5KD7GyJe')
ON CONFLICT (username) DO NOTHING;

INSERT INTO cliente (documento, nombre, telefono) VALUES
('10101010', 'Carlos Pérez', '3001234567')
ON CONFLICT (documento) DO NOTHING;

INSERT INTO vehiculo (cliente_id, placa, marca, modelo)
SELECT id, 'AAA123', 'Yamaha', 'FZ25'
FROM cliente WHERE documento = '10101010'
ON CONFLICT (placa) DO NOTHING;

INSERT INTO proveedor (nit, nombre, telefono, email, direccion, tiempo_entrega_dias) VALUES
('900123456-1', 'Distribuidora Motopartes S.A.S.', '3105550199', 'ventas@motopartes.com', 'Calle 13 # 24-50, Bogotá', 2),
('800987654-2', 'Importadora Yamaha & Racing', '3204448877', 'pedidos@yamaharacing.co', 'Av. Boyacá # 68-12, Bogotá', 0)
ON CONFLICT (nit) DO NOTHING;

INSERT INTO pieza (sku, nombre, categoria, precio_venta, stock_actual, stock_minimo) VALUES
('REP-001', 'Pastillas de Freno', 'Repuesto', 45000.00, 10, 2),
('ACC-001', 'Casco Integral', 'Accesorio', 120000.00, 5, 1)
ON CONFLICT (sku) DO NOTHING;

INSERT INTO pieza_proveedor (pieza_id, proveedor_id, prioridad, costo_pactado)
SELECT p.id, pr.id, 1, 28000.00
FROM pieza p, proveedor pr
WHERE p.sku = 'REP-001' AND pr.nit = '900123456-1'
ON CONFLICT (pieza_id, proveedor_id) DO NOTHING;

INSERT INTO pieza_proveedor (pieza_id, proveedor_id, prioridad, costo_pactado)
SELECT p.id, pr.id, 2, 31000.00
FROM pieza p, proveedor pr
WHERE p.sku = 'REP-001' AND pr.nit = '800987654-2'
ON CONFLICT (pieza_id, proveedor_id) DO NOTHING;