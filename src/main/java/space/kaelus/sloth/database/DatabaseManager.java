package space.kaelus.sloth.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.config.ConfigManager;

@Getter
@Singleton
public class DatabaseManager {
  private final ViolationDatabase database;
  private final HikariDataSource dataSource;

  @Inject
  public DatabaseManager(SlothAC plugin, ConfigManager configManager) {
    this.dataSource = createDataSource(plugin);
    this.database = new SQLiteViolationDatabase(this.dataSource, plugin, configManager);
  }

  private HikariDataSource createDataSource(SlothAC plugin) {
    File dbFile = new File(plugin.getDataFolder(), "violations.db");

    HikariConfig config = new HikariConfig();
    config.setPoolName("Sloth-Pool");
    config.setDriverClassName("org.sqlite.JDBC");
    config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

    int poolSize = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
    config.setMaximumPoolSize(poolSize);

    config.addDataSourceProperty("journal_mode", "WAL");
    config.addDataSourceProperty("synchronous", "NORMAL");
    config.addDataSourceProperty("busy_timeout", "5000");

    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);

    return new HikariDataSource(config);
  }

  public void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }
}
