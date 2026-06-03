package com.openlist.app.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "token") val token: String = "",
    @ColumnInfo(name = "username") val username: String = "",
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "last_used") val lastUsed: Long = System.currentTimeMillis()
)

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY last_used DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveServer(): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity): Long

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("UPDATE servers SET is_active = 0")
    suspend fun clearActiveServer()

    @Query("UPDATE servers SET is_active = 1, last_used = :time WHERE id = :id")
    suspend fun setActiveServer(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE servers SET token = :token WHERE id = :id")
    suspend fun updateToken(id: Long, token: String)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getServerCount(): Int
}

@Database(entities = [ServerEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "openlist_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
