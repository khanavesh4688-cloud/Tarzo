package com.tarzo.ai.features.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tarzo.ai.core.network.ApiClient
import com.tarzo.ai.core.network.WeatherResponse
import com.tarzo.ai.core.storage.SecureStorage
import com.tarzo.ai.util.Constants
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured weather data presented to the UI.
 */
data class WeatherInfo(
    val temperature: String,
    val condition: String,
    val humidity: String,
    val location: String,
    val feelsLike: String
) {
    fun toReadableString(): String {
        return "In $location, the weather is $condition. " +
            "Temperature is $temperature (feels like $feelsLike). Humidity is $humidity."
    }

    companion object {
        fun fromResponse(response: WeatherResponse): WeatherInfo {
            return WeatherInfo(
                temperature = response.temperature.ifBlank { "N/A" },
                condition = response.condition.ifBlank { "unknown" },
                humidity = response.humidity.ifBlank { "N/A" },
                location = response.location.ifBlank { "your area" },
                feelsLike = response.feelsLike.ifBlank { "N/A" }
            )
        }
    }
}

/**
 * Structured news article data.
 */
data class NewsArticle(
    val title: String,
    val description: String,
    val source: String,
    val url: String,
    val publishedAt: String? = null
) {
    fun toReadableString(): String {
        val time = publishedAt?.let { " (${it})" } ?: ""
        return "$title\n$source$time: $description"
    }
}

/**
 * Manages web search, weather, and news briefing operations.
 * Uses [Intent.ACTION_WEB_SEARCH] as the primary search mechanism
 * and falls back to browser URLs when no handler is available.
 * If a backend API is configured, weather is fetched from the server.
 */
@Singleton
class SearchManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val apiClient: ApiClient
) {

    /**
     * Performs a web search by launching the default web search handler.
     * Falls back to opening a Google search URL in the browser.
     */
    fun webSearch(query: String): Result<String> {
        return try {
            if (query.isBlank()) {
                return Result.Error(
                    IllegalArgumentException("Empty query"),
                    "Search query cannot be empty."
                )
            }

            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
                Result.Success("Searching for: $query")
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Result.Success("Searching for: $query")
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to perform web search: ${e.message}")
        }
    }

    /**
     * Gets weather information. If a backend API is configured, fetches from the server.
     * Otherwise, opens a web search for current weather.
     */
    suspend fun getWeather(): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        try {
            if (apiClient.isConfigured()) {
                when (val response = apiClient.getWeather()) {
                    is Result.Success -> {
                        val info = WeatherInfo.fromResponse(response.data)
                        if (info.condition != "unknown" || info.temperature != "N/A") {
                            return@withContext Result.Success(info)
                        }
                    }
                    else -> { /* fall through to web search */ }
                }
            }

            // Fallback: open weather search in browser
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, "weather today current location")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            Result.Success(
                WeatherInfo(
                    temperature = "see browser",
                    condition = "check browser",
                    humidity = "N/A",
                    location = "",
                    feelsLike = "N/A"
                )
            )
        } catch (e: Exception) {
            try { webSearch("weather today") } catch (_: Exception) {}
            Result.Error(e, "Could not get weather: ${e.message}")
        }
    }

    /**
     * Gets a news briefing. Opens a web search for latest news headlines.
     * If a backend API is configured, could fetch from there in the future.
     */
    suspend fun getNewsBriefing(): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        try {
            // Try API-based web search if configured
            if (apiClient.isConfigured()) {
                when (val response = apiClient.searchWeb("latest news today India")) {
                    is Result.Success -> {
                        val articles = response.data.results.map { sr ->
                            NewsArticle(
                                title = sr.title,
                                description = sr.snippet,
                                source = sr.url.removePrefix("https://").split("/").firstOrNull() ?: "Web",
                                url = sr.url,
                                publishedAt = null
                            )
                        }
                        if (articles.isNotEmpty()) {
                            return@withContext Result.Success(articles)
                        }
                    }
                    else -> { /* fall through */ }
                }
            }

            // Fallback: open news search in browser
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, "latest news today India")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            Result.Success(emptyList())
        } catch (e: Exception) {
            try { webSearch("latest news today") } catch (_: Exception) {}
            Result.Error(e, "Could not get news: ${e.message}")
        }
    }

    /**
     * Returns the current date and time as a formatted string.
     */
    fun getDateTime(): String {
        val now = Date()
        val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val isLeap = (cal.get(Calendar.YEAR) % 4 == 0 && cal.get(Calendar.YEAR) % 100 != 0) ||
            (cal.get(Calendar.YEAR) % 400 == 0)
        val totalDays = if (isLeap) 366 else 365
        val remaining = totalDays - dayOfYear
        return "Today is $dateStr. The time is $timeStr. Day $dayOfYear of $totalDays. $remaining days remaining in the year."
    }
}
