package db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.sql.Time;
import java.util.List;

@Dao
public interface UserAppDao {
    @Query("SELECT * FROM UserApp")
    List<UserApp> getAll();

    @Query("SELECT * FROM UserApp WHERE id = (:id)")
    List<UserApp> loadAllByIds(int id);

    @Query("SELECT * FROM UserApp WHERE id = (:id) AND accessTime = (:time)")
    List<UserApp> loadAllByIds(int id, Time time);

    @Insert
    void insertAll(UserApp... app);

    @Delete
    void delete(UserApp... app);
}
