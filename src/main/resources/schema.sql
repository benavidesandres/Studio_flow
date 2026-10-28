-- StudioFlow - DDL Schema for PostgreSQL (Supabase)
-- Ejecutar en el SQL Editor de Supabase si se desea inicializar manualmente

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PRODUCER', 'DIRECTOR', 'CREW', 'ACTOR')),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS productions (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    total_budget NUMERIC(14, 2) DEFAULT 0.00,
    spent_budget NUMERIC(14, 2) DEFAULT 0.00,
    progress_percentage INTEGER DEFAULT 0,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS scenes (
    id BIGSERIAL PRIMARY KEY,
    production_id BIGINT NOT NULL REFERENCES productions(id) ON DELETE CASCADE,
    scene_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    scheduled_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('READY', 'DELAYED', 'PROBLEM', 'COMPLETED')),
    location VARCHAR(255) NOT NULL,
    cost NUMERIC(12, 2) DEFAULT 0.00,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS scene_actors (
    scene_id BIGINT NOT NULL REFERENCES scenes(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (scene_id, user_id)
);

-- Índices recomendados para optimización de consultas de agenda y conflictos
CREATE INDEX IF NOT EXISTS idx_scenes_production_id ON scenes(production_id);
CREATE INDEX IF NOT EXISTS idx_scenes_scheduled_time ON scenes(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_scene_actors_user_id ON scene_actors(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
