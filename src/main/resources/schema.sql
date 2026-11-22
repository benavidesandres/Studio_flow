-- StudioFlow - DDL Schema for PostgreSQL (Supabase)
-- Ejecutar en el SQL Editor de Supabase (https://mnuutpqvemaiwjnlvmcp.supabase.co)

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PRODUCER', 'DIRECTOR', 'CREW', 'ACTOR')),
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    character_name VARCHAR(255),
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
    status VARCHAR(50) DEFAULT 'FILMING',
    director_name VARCHAR(255),
    poster_url VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS scenes (
    id BIGSERIAL PRIMARY KEY,
    production_id BIGINT NOT NULL REFERENCES productions(id) ON DELETE CASCADE,
    scene_number VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    shoot_date DATE,
    start_time TIME WITHOUT TIME ZONE,
    end_time TIME WITHOUT TIME ZONE,
    scheduled_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('READY', 'DELAYED', 'PROBLEM', 'COMPLETED')),
    location VARCHAR(255) NOT NULL,
    cost NUMERIC(12, 2) DEFAULT 0.00,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    notes TEXT,
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
CREATE INDEX IF NOT EXISTS idx_scenes_shoot_date ON scenes(shoot_date);
CREATE INDEX IF NOT EXISTS idx_scene_actors_user_id ON scene_actors(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Datos iniciales de demostración (Seed Data)
-- Password por defecto para los usuarios de prueba: password123 ($2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lR.ZeVxQvG7P2k7Lq0q9k.hC9xNde)
INSERT INTO users (email, password, full_name, role, phone, avatar_url, character_name)
VALUES
  ('productor@studioflow.io', '$2a$10$tQWjPj5dE2rOiqJcKk650eEa5Vn1Y5b1cMvMhA2lMpq5F9B7Gv0te', 'Guillermo del Toro', 'PRODUCER', '+1 555-0199', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80', NULL),
  ('director@studioflow.io', '$2a$10$tQWjPj5dE2rOiqJcKk650eEa5Vn1Y5b1cMvMhA2lMpq5F9B7Gv0te', 'Denis Villeneuve', 'DIRECTOR', '+1 555-0188', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80', NULL),
  ('crew@studioflow.io', '$2a$10$tQWjPj5dE2rOiqJcKk650eEa5Vn1Y5b1cMvMhA2lMpq5F9B7Gv0te', 'Roger Deakins', 'CREW', '+1 555-0177', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80', NULL),
  ('actor1@studioflow.io', '$2a$10$tQWjPj5dE2rOiqJcKk650eEa5Vn1Y5b1cMvMhA2lMpq5F9B7Gv0te', 'Christian Bale', 'ACTOR', '+1 555-0101', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=300&q=80', 'Comandante Kaelen'),
  ('actor2@studioflow.io', '$2a$10$tQWjPj5dE2rOiqJcKk650eEa5Vn1Y5b1cMvMhA2lMpq5F9B7Gv0te', 'Ana de Armas', 'ACTOR', '+1 555-0102', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80', 'Dra. Elena Vasquez')
ON CONFLICT (email) DO NOTHING;

INSERT INTO productions (id, title, description, start_date, end_date, total_budget, spent_budget, progress_percentage, status, director_name, poster_url)
VALUES
  (1, 'Shadows of Neo-Tokyo', 'Thriller cyberpunk en un futuro distópico', '2026-10-25', '2026-12-15', 4500000.00, 3870000.00, 68, 'FILMING', 'Denis Villeneuve', 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80')
ON CONFLICT (id) DO NOTHING;
