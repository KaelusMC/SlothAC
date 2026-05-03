CREATE TABLE IF NOT EXISTS player_logins (
  uuid TEXT NOT NULL PRIMARY KEY,
  last_seen INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS player_logins_last_seen_idx ON player_logins (last_seen);
