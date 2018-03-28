package db;

import android.arch.persistence.room.Database;

@Database(entities = {UserApp.class}, version = 1)
public abstract class AppDatabase {
    public abstract UserAppDao userAppDao();
}
