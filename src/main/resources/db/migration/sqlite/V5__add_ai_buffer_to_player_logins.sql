ALTER TABLE player_logins ADD COLUMN ai_buffer REAL NOT NULL DEFAULT 0;
ALTER TABLE player_logins ADD COLUMN ai_buffer_updated_at INTEGER NOT NULL DEFAULT 0;
