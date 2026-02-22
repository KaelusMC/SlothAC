package space.kaelus.sloth.database

import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertTrue
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

class FlywaySqliteMigrationsTest {

  @Test
  fun `applies sqlite sql and java migrations`() {
    val databaseFile = Files.createTempFile("slothac-flyway-", ".db").toFile()
    databaseFile.deleteOnExit()
    val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    Flyway.configure()
      .dataSource(jdbcUrl, null, null)
      .locations("classpath:db/migration/common", "classpath:db/migration/sqlite")
      .baselineOnMigrate(true)
      .baselineVersion("0")
      .load()
      .migrate()

    DriverManager.getConnection(jdbcUrl).use { connection ->
      assertTrue(tableExists(connection, "violations"))
      assertTrue(tableExists(connection, "sloth_punishments"))
      assertTrue(tableExists(connection, "monitor_settings"))
      assertTrue(columnExists(connection, "violations", "created_at_instant"))
      assertTrue(columnExists(connection, "monitor_settings", "show_name"))
      val versions = appliedVersions(connection)
      assertTrue(versions.contains("1"))
      assertTrue(versions.contains("2"))
    }
  }

  private fun tableExists(connection: Connection, tableName: String): Boolean {
    connection.metaData.getTables(null, null, tableName, arrayOf("TABLE")).use { resultSet ->
      return resultSet.next()
    }
  }

  private fun columnExists(connection: Connection, tableName: String, columnName: String): Boolean {
    connection.metaData.getColumns(null, null, tableName, columnName).use { resultSet ->
      return resultSet.next()
    }
  }

  private fun appliedVersions(connection: Connection): Set<String> {
    val versions = mutableSetOf<String>()
    connection.createStatement().use { statement ->
      statement
        .executeQuery(
          "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL"
        )
        .use { resultSet ->
          while (resultSet.next()) {
            resultSet.getString("version")?.takeIf { it.isNotBlank() }?.let(versions::add)
          }
        }
    }
    return versions
  }
}
