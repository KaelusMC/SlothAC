CREATE TABLE IF NOT EXISTS violations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  server VARCHAR(255) NOT NULL,
  uuid VARCHAR(36) NOT NULL,
  player_name VARCHAR(64) NOT NULL,
  check_name VARCHAR(255) NOT NULL,
  verbose TEXT NOT NULL,
  vl INT NOT NULL,
  created_at BIGINT NOT NULL,
  created_at_instant DATETIME(3) NOT NULL DEFAULT '1970-01-01 00:00:00.000',
  PRIMARY KEY (id),
  INDEX violations_uuid_created_at_idx (uuid, created_at),
  INDEX violations_created_at_idx (created_at),
  INDEX violations_uuid_created_at_instant_idx (uuid, created_at_instant),
  INDEX violations_created_at_instant_idx (created_at_instant)
);

CREATE TABLE IF NOT EXISTS sloth_punishments (
  uuid VARCHAR(36) NOT NULL,
  punish_group VARCHAR(255) NOT NULL,
  vl INT NOT NULL,
  PRIMARY KEY (uuid, punish_group)
);

CREATE TABLE IF NOT EXISTS monitor_settings (
  uuid VARCHAR(36) NOT NULL,
  mode VARCHAR(16) NOT NULL,
  theme VARCHAR(16) NOT NULL,
  show_ping BOOLEAN NOT NULL,
  show_dmg BOOLEAN NOT NULL,
  show_trend BOOLEAN NOT NULL,
  show_name VARCHAR(16) NOT NULL DEFAULT 'AUTO',
  PRIMARY KEY (uuid)
);
