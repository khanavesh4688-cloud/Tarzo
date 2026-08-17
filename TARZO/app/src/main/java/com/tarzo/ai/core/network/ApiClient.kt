package com.tarzo.ai.core.network

import com.tarzo.ai.core.storage.SecureStorage
import com.tarzo.ai.util.Constants
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Data classes for API responses ────────────────────────────

data class WebSearchResponse(
    val results: List<SearchResult> = emptyList(),
    val totalResults: Int = 0
)

data class SearchResult(
    val title: String = "",
    val snippet: String = "",
    val url: String = ""
)

data class WeatherResponse(
    val temperature: String = "",
    val condition: String = "",
    val humidity: String = "",
    val location: String = "",
    val feelsLike: String = ""
)

data class TranslateResponse(
    val translatedText: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = ""
)

// ── API Service Interface ─────────────────────────────────────

interface TarzoApiService {
    @GET(Constants.ENDPOINT_WEB_SEARCH)
    suspend fun searchWeb(
        @Query("q") query: String,
        @Query("lang") language: String = "hi"
    ): WebSearchResponse

    @GET(Constants.ENDPOINT_WEATHER)
    suspend fun getWeather(
        @Query("location") location: String = "",
        @Query("lat") latitude: Double? = null,
        @Query("lon") longitude: Double? = null
    ): WeatherResponse

    @GET(Constants.ENDPOINT_TRANSLATE)
    suspend fun translate(
        @Query("text") text: String,
        @Query("target") targetLanguage: String
    ): TranslateResponse
}

/**
 * Retrofit-based API client for TARZO.
 * Base URL is configurable — defaults to empty, meaning a backend server
 * must be set up for AI features. The client handles errors gracefully.
 */
@Singleton
class ApiClient @Inject constructor(
    private val secureStorage: SecureStorage
) {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val baseUrl = secureStorage.getApiBaseUrl()
                val requestBuilder = originalRequest.newBuilder()
                    .header("X-Client-Version", "1.0.0")
                    .header("Accept", "application/json")

                // Inject user API key if available
                val apiKey = secureStorage.getApiKey()
                if (!apiKey.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $apiKey")
                }

                if (baseUrl.isBlank()) {
                    return@interceptor chain.proceed(requestBuilder.build())
                }
                val newUrl = originalRequest.url.newBuilder()
                    .scheme("https")
                    .host(baseUrl.removePrefix("https://").removePrefix("http://").split("/").first())
                    .build()
                requestBuilder.url(newUrl)
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val baseUrl = secureStorage.getApiBaseUrl().ifBlank {
            "https://localhost"
        }
        Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: TarzoApiService by lazy {
        retrofit.create(TarzoApiService::class.java)
    }

    fun updateBaseUrl(baseUrl: String) {
        secureStorage.saveApiBaseUrl(baseUrl)
    }

    suspend fun searchWeb(
        query: String,
        language: String = "hi"
    ): Result<WebSearchResponse> {
        return safeApiCall {
            apiService.searchWeb(query, language)
        }
    }

    suspend fun getWeather(
        location: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<WeatherResponse> {
        return safeApiCall {
            apiService.getWeather(
                location = location ?: "",
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    suspend fun translate(
        text: String,
        targetLanguage: String
    ): Result<TranslateResponse> {
        return safeApiCall {
            apiService.translate(text, targetLanguage)
        }
    }

    fun isConfigured(): Boolean {
        return secureStorage.getApiBaseUrl().isNotBlank()
    }

    fun getCurrentBaseUrl(): String {
        return secureStorage.getApiBaseUrl()
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            if (!isConfigured()) {
                return@withContext Result.Error(
                    IllegalStateException("Backend not configured"),
                    "API server ka address set nahi hai. Settings mein jaake configure karo."
                )
            }
            try {
                val response = apiCall()
                Result.Success(response)
            } catch (e: HttpException) {
                val message = when (e.code()) {
                    401 -> "Authentication failed. Server key invalid hai."
                    403 -> "Access denied. Permission nahi hai."
                    404 -> "Endpoint nahi mila. Server check karo."
                    429 -> "Bahut zyada requests. Thodi der baad try karo."
                    in 500..599 -> "Server mein problem hai. Baad mein try karo."
                    else -> "Network error: ${e.code()}"
                }
                Result.Error(e, message)
            } catch (e: SocketTimeoutException) {
                Result.Error(e, "Server ka jawab nahi aaya. Internet check karo.")
            } catch (e: UnknownHostException) {
                Result.Error(e, "Server se connect nahi ho paya. URL check karo.")
            } catch (e: Exception) {
                Result.Error(e, "Kuch gadbad ho gayi: ${e.message}")
            }
        }
    }

    companion object {
        const val TIMEOUT_SECONDS = 15L
    }
}
