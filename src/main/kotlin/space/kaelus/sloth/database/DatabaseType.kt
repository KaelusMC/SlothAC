package space.kaelus.sloth.database

import java.util.Locale

enum class DatabaseType(val flywayLocation: String) {
  SQLITE("sqlite"),
  MYSQL("mysql");

  companion object {
    fun fromConfig(raw: String?): DatabaseType {
      if (raw == null) {
        return SQLITE
      }
      return when (raw.trim().lowercase(Locale.ROOT)) {
        "mysql" -> MYSQL
        "sqlite" -> SQLITE
        else -> SQLITE
      }
    }
  }
}
