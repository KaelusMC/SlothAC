@file:Suppress("ClassName")

package db.migration.sqlite

import java.sql.Connection
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V3__LegacySchemaPatch : BaseJavaMigration() {
  override fun migrate(context: Context) {
    val connection = context.connection
    if (!tableExists(connection, VIOLATIONS_TABLE)) {
      return
    }

    ensureCreatedAtInstantColumn(connection)
    backfillCreatedAtInstant(connection)
  }

  private fun ensureCreatedAtInstantColumn(connection: Connection) {
    if (columnExists(connection, VIOLATIONS_TABLE, CREATED_AT_INSTANT_COLUMN)) {
      return
    }
    val columnDefinition = "TEXT NOT NULL DEFAULT '$SQLITE_EPOCH_INSTANT'"
    connection.createStatement().use { statement ->
      statement.executeUpdate(
        "ALTER TABLE $VIOLATIONS_TABLE ADD COLUMN $CREATED_AT_INSTANT_COLUMN $columnDefinition"
      )
    }
  }

  private fun backfillCreatedAtInstant(connection: Connection) {
    if (
      !columnExists(connection, VIOLATIONS_TABLE, CREATED_AT_COLUMN) ||
        !columnExists(connection, VIOLATIONS_TABLE, CREATED_AT_INSTANT_COLUMN)
    ) {
      return
    }
    connection.createStatement().use { statement ->
      statement.executeUpdate(
        """
        UPDATE $VIOLATIONS_TABLE
        SET $CREATED_AT_INSTANT_COLUMN = strftime('%Y-%m-%d %H:%M:%f', $CREATED_AT_COLUMN / 1000.0, 'unixepoch')
        WHERE $CREATED_AT_INSTANT_COLUMN = '$SQLITE_EPOCH_INSTANT'
        """
          .trimIndent()
      )
    }
  }

  private fun tableExists(connection: Connection, tableName: String): Boolean {
    connection
      .prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1")
      .use { statement ->
        statement.setString(1, tableName)
        statement.executeQuery().use { resultSet ->
          return resultSet.next()
        }
      }
  }

  private fun columnExists(connection: Connection, tableName: String, columnName: String): Boolean {
    connection.metaData.getColumns(null, null, tableName, columnName).use { resultSet ->
      return resultSet.next()
    }
  }

  private companion object {
    const val VIOLATIONS_TABLE = "violations"
    const val CREATED_AT_COLUMN = "created_at"
    const val CREATED_AT_INSTANT_COLUMN = "created_at_instant"
    const val SQLITE_EPOCH_INSTANT = "1970-01-01 00:00:00.000"
  }
}
