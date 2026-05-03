CREATE TABLE IF NOT EXISTS player_logins (
  uuid VARCHAR(36) NOT NULL,
  last_seen BIGINT NOT NULL,
  PRIMARY KEY (uuid),
  INDEX player_logins_last_seen_idx (last_seen)
);
