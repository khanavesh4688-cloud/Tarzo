package com.tarzo.ai.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing persistent memory items using Room database.
 * Provides a clean API over MemoryDao for the rest of the app.
 */
@Singleton
class MemoryManager @Inject constructor(
    private val memoryDao: MemoryDao
) {
    fun getAllMemories(): Flow<List<MemoryItem>> {
        return memoryDao.getAll().flowOn(Dispatchers.IO)
    }

    fun getMemoriesByCategory(category: String): Flow<List<MemoryItem>> {
        return memoryDao.getByCategory(category).flowOn(Dispatchers.IO)
    }

    fun searchMemories(query: String): Flow<List<MemoryItem>> {
        return memoryDao.searchByContent(query).flowOn(Dispatchers.IO)
    }

    suspend fun remember(
        content: String,
        category: String = MemoryItem.CATEGORY_FACT
    ): Long {
        return withContext(Dispatchers.IO) {
            val item = MemoryItem(
                content = content.trim(),
                category = validateCategory(category),
                timestamp = System.currentTimeMillis()
            )
            memoryDao.insert(item)
        }
    }

    suspend fun rememberContact(name: String, info: String = ""): Long {
        val content = if (info.isNotBlank()) {
            "$name - $info"
        } else {
            name
        }
        return remember(content, MemoryItem.CATEGORY_CONTACT)
    }

    suspend fun rememberPreference(key: String, value: String): Long {
        return remember("$key = $value", MemoryItem.CATEGORY_PREFERENCE)
    }

    suspend fun rememberSetting(key: String, value: String): Long {
        return remember("$key = $value", MemoryItem.CATEGORY_SETTING)
    }

    suspend fun forget(id: Long) {
        withContext(Dispatchers.IO) {
            memoryDao.deleteById(id)
        }
    }

    suspend fun forgetByContent(keyword: String): Int {
        return withContext(Dispatchers.IO) {
            memoryDao.deleteByContent(keyword)
        }
    }

    suspend fun getMemoryById(id: Long): MemoryItem? {
        return withContext(Dispatchers.IO) {
            memoryDao.getById(id)
        }
    }

    suspend fun getTotalCount(): Int {
        return withContext(Dispatchers.IO) {
            memoryDao.getCount()
        }
    }

    suspend fun getCountByCategory(category: String): Int {
        return withContext(Dispatchers.IO) {
            memoryDao.getCountByCategory(category)
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            memoryDao.deleteAll()
        }
    }

    suspend fun findPreference(key: String): String? {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<MemoryItem>()
            val flow = memoryDao.searchByContent(key)
            flow.collect { items ->
                results.addAll(items)
            }
            results
                .filter { it.category == MemoryItem.CATEGORY_PREFERENCE && it.content.startsWith(key) }
                .firstOrNull()
                ?.content
                ?.removePrefix("$key = ")
        }
    }

    private fun validateCategory(category: String): String {
        return when (category.uppercase()) {
            MemoryItem.CATEGORY_PREFERENCE -> MemoryItem.CATEGORY_PREFERENCE
            MemoryItem.CATEGORY_FACT -> MemoryItem.CATEGORY_FACT
            MemoryItem.CATEGORY_CONTACT -> MemoryItem.CATEGORY_CONTACT
            MemoryItem.CATEGORY_SETTING -> MemoryItem.CATEGORY_SETTING
            else -> MemoryItem.CATEGORY_FACT
        }
    }
}