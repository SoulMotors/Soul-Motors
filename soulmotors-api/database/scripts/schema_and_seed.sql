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
    rol VARCHAR(50) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'GERENTE', 'MECANICO', 'VENDEDOR')),
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
-- TAREA S2-T01: ÓRDENES DE TRABAJO, DETALLE Y GARANTÍAS
-- Entorno: Neon (PostgreSQL 17 Cloud)
-- Rama: feature/db-sprint2-ordenes
-- ==========================================

-- 1. Tabla Cabecera: orden_trabajo
CREATE TABLE orden_trabajo (
    id BIGSERIAL PRIMARY KEY,
    vehiculo_id BIGINT NOT NULL,
    mecanico_asignado_id BIGINT NOT NULL,
    vendedor_id BIGINT,
    orden_padre_garantia_id BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'DIAGNOSTICO'
        CHECK (estado IN ('DIAGNOSTICO', 'PENDIENTE', 'EN_PROCESO', 'FINALIZADA', 'ENTREGADA', 'CANCELADA')),
    descripcion_ingreso TEXT NOT NULL,
    viabilidad_tecnica BOOLEAN NOT NULL DEFAULT TRUE,
    costo_mano_obra NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_mano_obra >= 0),
    total_orden NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_orden >= 0),
    motivo_cancelacion TEXT,
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Llaves foráneas y reglas de integridad referencial
    CONSTRAINT fk_ot_vehiculo 
        FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ot_mecanico 
        FOREIGN KEY (mecanico_asignado_id) REFERENCES usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ot_vendedor 
        FOREIGN KEY (vendedor_id) REFERENCES usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ot_padre_garantia 
        FOREIGN KEY (orden_padre_garantia_id) REFERENCES orden_trabajo(id) ON DELETE RESTRICT,

    -- Si pasa a CANCELADA, exige motivo obligatorio (RF-ORD-03)
    CONSTRAINT chk_ot_motivo_cancelacion 
        CHECK (estado <> 'CANCELADA' OR (motivo_cancelacion IS NOT NULL AND LENGTH(TRIM(motivo_cancelacion)) > 0))
);

CREATE TRIGGER trg_orden_trabajo_actualizado_at
BEFORE UPDATE ON orden_trabajo
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- Índices de rendimiento para consultas frecuentes de Luisa (BE1) y Neyer (BE2)
CREATE INDEX idx_ot_vehiculo ON orden_trabajo(vehiculo_id);
CREATE INDEX idx_ot_mecanico_estado ON orden_trabajo(mecanico_asignado_id, estado);
CREATE INDEX idx_ot_padre_garantia ON orden_trabajo(orden_padre_garantia_id) WHERE orden_padre_garantia_id IS NOT NULL;


-- 2. Tabla Detalle: detalle_orden (Piezas solicitadas / aprobadas por OT)
CREATE TABLE detalle_orden (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    pieza_id BIGINT NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario_historico NUMERIC(12,2) CHECK (precio_unitario_historico >= 0),
    cubierto_por_garantia BOOLEAN NOT NULL DEFAULT FALSE,
    estado_solicitud VARCHAR(20) NOT NULL DEFAULT 'SOLICITADA'
        CHECK (estado_solicitud IN ('SOLICITADA', 'APROBADA', 'RECHAZADA', 'REVERTIDA')),
    aprobado_por_id BIGINT,
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Llaves foráneas
    CONSTRAINT fk_det_orden 
        FOREIGN KEY (orden_id) REFERENCES orden_trabajo(id) ON DELETE CASCADE,
    CONSTRAINT fk_det_pieza 
        FOREIGN KEY (pieza_id) REFERENCES pieza(id) ON DELETE RESTRICT,
    CONSTRAINT fk_det_aprobado_por 
        FOREIGN KEY (aprobado_por_id) REFERENCES usuario(id) ON DELETE RESTRICT,

    -- Cuando se aprueba la pieza, debe quedar congelado el precio histórico y quién aprobó
    CONSTRAINT chk_det_aprobacion_completa 
        CHECK (
            estado_solicitud <> 'APROBADA' 
            OR (precio_unitario_historico IS NOT NULL AND aprobado_por_id IS NOT NULL)
        )
);

CREATE TRIGGER trg_detalle_orden_actualizado_at
BEFORE UPDATE ON detalle_orden
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE INDEX idx_detalle_orden_orden ON detalle_orden(orden_id);
CREATE INDEX idx_detalle_orden_pieza ON detalle_orden(pieza_id);


-- 3. Tabla Certificado y Checklist Técnico: garantia_orden (Relación 1:1 con orden_trabajo)
CREATE TABLE garantia_orden (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL UNIQUE,
    km_entrega INT NOT NULL CHECK (km_entrega >= 0),
    dias_cobertura INT NOT NULL DEFAULT 30 CHECK (dias_cobertura > 0),
    km_limite_cobertura INT NOT NULL CHECK (km_limite_cobertura >= km_entrega),
    fecha_vencimiento DATE NOT NULL,
    
    -- Los 5 checks técnicos obligatorios de respaldo (E-02 / RF-ORD-04)
    chk_torque_seguridad BOOLEAN NOT NULL DEFAULT FALSE,
    chk_frenos_transmision BOOLEAN NOT NULL DEFAULT FALSE,
    chk_sistema_electrico BOOLEAN NOT NULL DEFAULT FALSE,
    chk_estanqueidad_empaques BOOLEAN NOT NULL DEFAULT FALSE,
    chk_prueba_ruta_aprobada BOOLEAN NOT NULL DEFAULT FALSE,
    
    series_lotes_piezas TEXT,
    observaciones_tecnicas TEXT,
    estado_garantia VARCHAR(30) NOT NULL DEFAULT 'VIGENTE'
        CHECK (estado_garantia IN ('VIGENTE', 'VENCIDA', 'RECLAMADA', 'ANULADA_POR_MANIPULACION')),
    creado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Llave foránea 1:1 hacia orden_trabajo
    CONSTRAINT fk_garantia_orden 
        FOREIGN KEY (orden_id) REFERENCES orden_trabajo(id) ON DELETE CASCADE
);

CREATE TRIGGER trg_garantia_orden_actualizado_at
BEFORE UPDATE ON garantia_orden
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE INDEX idx_garantia_estado_vencimiento ON garantia_orden(estado_garantia, fecha_vencimiento);


-- ==========================================
-- TAREA S2-T02: LÓGICA TRANSACCIONAL, GARANTÍAS Y AUDITORÍA
-- Entorno: Neon (PostgreSQL 17 Cloud)
-- ==========================================

-- 1. Función SQL de Descuento Atómico al Aprobar Piezas (RF-INV-02 / FL-04)
CREATE OR REPLACE FUNCTION fn_aprobar_pieza_orden(
    p_detalle_id BIGINT,
    p_admin_id BIGINT
) RETURNS VOID AS $$
DECLARE
    v_rol_admin VARCHAR(50);
    v_activo_admin BOOLEAN;
    v_orden_id BIGINT;
    v_orden_estado VARCHAR(20);
    v_pieza_id BIGINT;
    v_pieza_nombre VARCHAR(100);
    v_cantidad INT;
    v_estado_solicitud VARCHAR(20);
    v_stock_actual INT;
    v_precio_venta NUMERIC(12,2);
BEGIN
    -- Validar que el aprobador exista, esté activo y sea ADMINISTRADOR
    SELECT rol, activo INTO v_rol_admin, v_activo_admin
    FROM usuario WHERE id = p_admin_id;

    IF NOT FOUND OR v_activo_admin IS NOT TRUE OR v_rol_admin <> 'ADMINISTRADOR' THEN
        RAISE EXCEPTION 'Acceso denegado: solo un ADMINISTRADOR activo puede aprobar piezas (usuario_id=%).', p_admin_id;
    END IF;

    -- Bloquear línea de detalle_orden para evitar doble aprobación concurrente
    SELECT orden_id, pieza_id, cantidad, estado_solicitud
    INTO v_orden_id, v_pieza_id, v_cantidad, v_estado_solicitud
    FROM detalle_orden
    WHERE id = p_detalle_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el detalle de orden con id=%.', p_detalle_id;
    END IF;

    IF v_estado_solicitud <> 'SOLICITADA' THEN
        RAISE EXCEPTION 'Solo se pueden aprobar piezas en estado SOLICITADA (estado actual: %).', v_estado_solicitud;
    END IF;

    -- Validar que la orden no esté cerrada ni cancelada
    SELECT estado INTO v_orden_estado FROM orden_trabajo WHERE id = v_orden_id;
    IF v_orden_estado IN ('FINALIZADA', 'ENTREGADA', 'CANCELADA') THEN
        RAISE EXCEPTION 'No se pueden aprobar piezas en una orden con estado %.', v_orden_estado;
    END IF;

    -- Bloquear registro de pieza (FOR UPDATE) para garantizar atomicidad en stock
    SELECT nombre, stock_actual, precio_venta
    INTO v_pieza_nombre, v_stock_actual, v_precio_venta
    FROM pieza
    WHERE id = v_pieza_id
    FOR UPDATE;

    IF v_stock_actual < v_cantidad THEN
        RAISE EXCEPTION 'Stock insuficiente para "%" (disponible: %, solicitado: %, faltante: %).',
            v_pieza_nombre, v_stock_actual, v_cantidad, (v_cantidad - v_stock_actual);
    END IF;

    -- 1) Descontar inventario
    UPDATE pieza
    SET stock_actual = stock_actual - v_cantidad
    WHERE id = v_pieza_id;

    -- 2) Aprobar solicitud y congelar precio_unitario_historico
    UPDATE detalle_orden
    SET estado_solicitud = 'APROBADA',
        precio_unitario_historico = v_precio_venta,
        aprobado_por_id = p_admin_id
    WHERE id = p_detalle_id;

    -- 3) Registrar salida en movimiento_inventario
    INSERT INTO movimiento_inventario (pieza_id, tipo_movimiento, cantidad)
    VALUES (v_pieza_id, 'Salida', v_cantidad);

    -- 4) Registrar trazabilidad explícita en registro_auditoria
    INSERT INTO registro_auditoria (tabla_afectada, accion, usuario_id, detalle)
    VALUES (
        'detalle_orden',
        'APROBACION_PIEZA',
        p_admin_id,
        jsonb_build_object(
            'detalle_id', p_detalle_id,
            'orden_id', v_orden_id,
            'pieza_id', v_pieza_id,
            'cantidad', v_cantidad,
            'precio_congelado', v_precio_venta,
            'stock_restante', (v_stock_actual - v_cantidad)
        )
    );
END;
$$ LANGUAGE plpgsql;


-- 2. Trigger de Reverso Automático al Pasar a CANCELADA (HU-09)
CREATE OR REPLACE FUNCTION fn_reverso_cancelacion_ot()
RETURNS TRIGGER AS $$
DECLARE
    r_detalle RECORD;
BEGIN
    IF NEW.estado = 'CANCELADA' AND OLD.estado <> 'CANCELADA' THEN
        FOR r_detalle IN
            SELECT id, pieza_id, cantidad
            FROM detalle_orden
            WHERE orden_id = NEW.id AND estado_solicitud = 'APROBADA'
            FOR UPDATE
        LOOP
            -- Devolver exactamente las piezas descontadas al inventario
            UPDATE pieza
            SET stock_actual = stock_actual + r_detalle.cantidad
            WHERE id = r_detalle.pieza_id;

            -- Marcar la línea como REVERTIDA
            UPDATE detalle_orden
            SET estado_solicitud = 'REVERTIDA'
            WHERE id = r_detalle.id;

            -- Registrar entrada de devolución en movimiento_inventario
            INSERT INTO movimiento_inventario (pieza_id, tipo_movimiento, cantidad)
            VALUES (r_detalle.pieza_id, 'Entrada', r_detalle.cantidad);

            -- Registrar evento de reverso en registro_auditoria
            INSERT INTO registro_auditoria (tabla_afectada, accion, usuario_id, detalle)
            VALUES (
                'orden_trabajo',
                'REVERSO_STOCK',
                NEW.vendedor_id,
                jsonb_build_object(
                    'orden_id', NEW.id,
                    'detalle_id', r_detalle.id,
                    'pieza_id', r_detalle.pieza_id,
                    'cantidad_devuelta', r_detalle.cantidad,
                    'motivo_cancelacion', NEW.motivo_cancelacion
                )
            );
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reverso_cancelacion_ot
AFTER UPDATE OF estado ON orden_trabajo
FOR EACH ROW EXECUTE FUNCTION fn_reverso_cancelacion_ot();


-- 3. Función de Validación Automática de Vigencia de Garantía (Fecha y Kilometraje)
CREATE OR REPLACE FUNCTION fn_validar_vigencia_garantia(
    p_garantia_id BIGINT,
    p_km_actual INT,
    p_fecha_consulta DATE DEFAULT CURRENT_DATE
) RETURNS TABLE (
    garantia_id BIGINT,
    orden_id BIGINT,
    es_valida BOOLEAN,
    estado_actualizado VARCHAR(30),
    motivo_resultado TEXT
) AS $$
DECLARE
    v_garantia garantia_orden%ROWTYPE;
    v_nuevo_estado VARCHAR(30);
    v_motivo TEXT;
    v_valida BOOLEAN := FALSE;
BEGIN
    SELECT * INTO v_garantia FROM garantia_orden WHERE id = p_garantia_id FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe certificado de garantía con id=%.', p_garantia_id;
    END IF;

    IF v_garantia.estado_garantia IN ('RECLAMADA', 'ANULADA_POR_MANIPULACION') THEN
        v_nuevo_estado := v_garantia.estado_garantia;
        v_motivo := 'Garantía no elegible: estado actual ' || v_garantia.estado_garantia;
    ELSIF p_fecha_consulta > v_garantia.fecha_vencimiento THEN
        v_nuevo_estado := 'VENCIDA';
        v_motivo := FORMAT('Garantía vencida por tiempo (venció el %s, fecha consulta %s).', v_garantia.fecha_vencimiento, p_fecha_consulta);
    ELSIF p_km_actual > v_garantia.km_limite_cobertura THEN
        v_nuevo_estado := 'VENCIDA';
        v_motivo := FORMAT('Garantía vencida por kilometraje (límite %s km, actual %s km).', v_garantia.km_limite_cobertura, p_km_actual);
    ELSE
        v_nuevo_estado := 'VIGENTE';
        v_valida := TRUE;
        v_motivo := FORMAT('Garantía vigente hasta %s o %s km.', v_garantia.fecha_vencimiento, v_garantia.km_limite_cobertura);
    END IF;

    -- Actualizar estado automáticamente si cambió a VENCIDA
    IF v_nuevo_estado <> v_garantia.estado_garantia THEN
        UPDATE garantia_orden
        SET estado_garantia = v_nuevo_estado
        WHERE id = p_garantia_id;
    END IF;

    RETURN QUERY SELECT v_garantia.id, v_garantia.orden_id, v_valida, v_nuevo_estado, v_motivo;
END;
$$ LANGUAGE plpgsql;


-- 4. Función y Triggers Genéricos de Audit Log en Tablas Críticas (CAM-02)
CREATE OR REPLACE FUNCTION fn_audit_log_generico()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO registro_auditoria (tabla_afectada, accion, detalle)
        VALUES (TG_TABLE_NAME, 'INSERT', jsonb_build_object('registro_id', NEW.id, 'nuevo', to_jsonb(NEW)));
        RETURN NEW;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO registro_auditoria (tabla_afectada, accion, detalle)
        VALUES (TG_TABLE_NAME, 'UPDATE', jsonb_build_object('registro_id', NEW.id, 'anterior', to_jsonb(OLD), 'nuevo', to_jsonb(NEW)));
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO registro_auditoria (tabla_afectada, accion, detalle)
        VALUES (TG_TABLE_NAME, 'DELETE', jsonb_build_object('registro_id', OLD.id, 'anterior', to_jsonb(OLD)));
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_orden_trabajo
AFTER INSERT OR UPDATE OR DELETE ON orden_trabajo
FOR EACH ROW EXECUTE FUNCTION fn_audit_log_generico();

CREATE TRIGGER trg_audit_detalle_orden
AFTER INSERT OR UPDATE OR DELETE ON detalle_orden
FOR EACH ROW EXECUTE FUNCTION fn_audit_log_generico();

CREATE TRIGGER trg_audit_garantia_orden
AFTER INSERT OR UPDATE OR DELETE ON garantia_orden
FOR EACH ROW EXECUTE FUNCTION fn_audit_log_generico();

CREATE TRIGGER trg_audit_pieza
AFTER INSERT OR UPDATE OR DELETE ON pieza
FOR EACH ROW EXECUTE FUNCTION fn_audit_log_generico();

-- ==========================================
-- DATOS SEMILLA (DINÁMICOS SIN IDs QUEMADOS)
-- ==========================================

-- Hash BCrypt de prueba para entorno dev (contraseña: "SoulMotors2026*")
INSERT INTO usuario (nombre, rol, username, password) VALUES
('Admin General', 'ADMINISTRADOR', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
('Mecánico Juan', 'MECANICO', 'empleado1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy')
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
