package com.tarzo.ai.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class MemoryItem(
    val id: Long = 0,
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

@Singleton
class MemoryDao @Inject constructor() {
    private val memories = MutableStateFlow(mutableListOf<MemoryItem>())
    private val nextId = java.util.concurrent.atomic.AtomicLong(1L)

    suspend fun insert(memoryItem: MemoryItem): Long {
        val newId = nextId.getAndIncrement()
        val item = memoryItem.copy(id = newId)
        memories.value.add(item)
        return newId
    }

    suspend fun insertAll(items: List<MemoryItem>): List<Long> {
        val ids = mutableListOf<Long>()
        for (item in items) { ids.add(insert(item)) }
        return ids
    }

    suspend fun delete(memoryItem: MemoryItem) {
        memories.value.removeAll { it.id == memoryItem.id }
    }

    suspend fun deleteById(id: Long) {
        memories.value.removeAll { it.id == id }
    }

    fun getAll(): Flow<List<MemoryItem>> = memories.map { it.toList() }

    fun getByCategory(category: String): Flow<List<MemoryItem>> =
        memories.map { list -> list.filter { it.category == category } }

    fun searchByContent(query: String): Flow<List<MemoryItem>> =
        memories.map { list -> list.filter { it.content.contains(query, ignoreCase = true) } }

    suspend fun getById(id: Long): MemoryItem? = memories.value.find { it.id == id }

    suspend fun getCount(): Int = memories.value.size

    suspend fun getCountByCategory(category: String): Int =
        memories.value.count { it.category == category }

    suspend fun deleteAll() { memories.value.clear() }
}

abstract class AppDatabase {
    abstract fun memoryDao(): MemoryDao
    companion object {
        const val DATABASE_VERSION = 1
    }
}