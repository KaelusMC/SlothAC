package space.kaelus.sloth.database;

import lombok.Getter;

@Getter
public class DatabaseManager {
    private ViolationDatabase database;

    public DatabaseManager() {
        this.database = new SQLiteViolationDatabase();
        this.database.connect();
    }

    public void shutdown() {
        if (database != null) {
            database.disconnect();
        }
    }
}