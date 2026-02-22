CREATE TABLE IF NOT EXISTS violations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  server TEXT NOT NULL,
  uuid TEXT NOT NULL,
  player_name TEXT NOT NULL,
  check_name TEXT NOT NULL,
  verbose TEXT NOT NULL,
  vl INTEGER NOT NULL,
  created_at INTEGER NOT NULL,
  created_at_instant TEXT NOT NULL DEFAULT '1970-01-01 00:00:00.000'
);

CREATE TABLE IF NOT EXISTS sloth_punishments (
  uuid TEXT NOT NULL,
  punish_group TEXT NOT NULL,
  vl INTEGER NOT NULL,
  PRIMARY KEY (uuid, punish_group)
);

CREATE TABLE IF NOT EXISTS monitor_settings (
  uuid TEXT NOT NULL PRIMARY KEY,
  mode TEXT NOT NULL,
  theme TEXT NOT NULL,
  show_ping INTEGER NOT NULL,
  show_dmg INTEGER NOT NULL,
  show_trend INTEGER NOT NULL,
  show_name TEXT NOT NULL DEFAULT 'AUTO'
);

CREATE INDEX IF NOT EXISTS violations_uuid_created_at_idx ON violations (uuid, created_at);
CREATE INDEX IF NOT EXISTS violations_created_at_idx ON violations (created_at);
CREATE INDEX IF NOT EXISTS violations_uuid_created_at_instant_idx
  ON violations (uuid, created_at_instant);
CREATE INDEX IF NOT EXISTS violations_created_at_instant_idx ON violations (created_at_instant);
