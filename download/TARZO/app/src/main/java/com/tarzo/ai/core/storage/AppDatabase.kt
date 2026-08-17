package com.tarzo.ai.core.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Entity(
    tableName = "memories",
    indices = [Index(value = ["category"]), Index(value = ["timestamp"])]
)
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: String = CATEGORY_FACT,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_PREFERENCE = "PREFERENCE"
        const val CATEGORY_FACT = "FACT"
        const val CATEGORY_CONTACT = "CONTACT"
        const val CATEGORY_SETTING = "SETTING"
    }
}

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memoryItem: MemoryItem): Long

    @Insert
    suspend fun insertAll(items: List<MemoryItem>): List<Long>

    @Delete
    suspend fun delete(memoryItem: MemoryItem)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories WHERE content LIKE :keyword")
    suspend fun deleteByContent(keyword: String): Int

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchByContent(query: String): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: Long): MemoryItem?

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM memories WHERE category = :category")
    suspend fun getCountByCategory(category: String): Int

    @Query("DELETE FROM memories")
    suspend fun deleteAll()
}

@Database(
    entities = [MemoryItem::class],
    version = DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        const val DATABASE_VERSION = 1

        val MIGRATIONS = listOf<Migration>(
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMemoryDao(database: AppDatabase): MemoryDao {
        return database.memoryDao()
    }
}
