package com.tarzo.ai.core.ai

import android.util.Log
import com.tarzo.ai.core.storage.SecureStorage
import com.tarzo.ai.util.Constants.IntentType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── OpenAI-compatible Chat API data classes ─────────────────────

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 200,
    val stream: Boolean = false
)

data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage,
    @SerializedName("finish_reason")
    val finishReason: String = "stop"
)

data class ChatResponse(
    val id: String = "",
    val choices: List<ChatChoice> = emptyList(),
    @SerializedName("usage")
    val usage: Usage? = null
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)

/**
 * Generates natural-language responses for detected intents.
 * When an API key and base URL are configured, makes real LLM calls
 * via OpenAI-compatible chat completions endpoint.
 * Falls back to offline rule-based responses otherwise.
 */
@Singleton
class LLMClient @Inject constructor(
    private val secureStorage: SecureStorage
) {
    private val gson = Gson()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Callback for streaming partial responses to UI
    var onStreamChunk: ((String) -> Unit)? = null

    // Conversation history for multi-turn context (last N messages)
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val MAX_HISTORY = 10

    /**
 * Synchronous (offline) response generation. Used as fallback.
     */
    fun generateResponse(
        query: String,
        intentResult: IntentResult,
        language: String = "hi-IN"
    ): String {
        val responseKey = ResponseKey(intentResult.intent, language)
        return responseMap[responseKey]
            ?: generateDynamicResponse(query, intentResult, language)
    }

    /**
     * Coroutine-based response generation.
     * Tries the LLM API first; falls back to offline rules on failure.
     */
    suspend fun generateResponseAsync(
        query: String,
        intentResult: IntentResult,
        language: String = "hi-IN"
    ): String {
        val apiKey = secureStorage.getApiKey()
        val baseUrl = secureStorage.getApiBaseUrl()

        if (apiKey.isNullOrBlank() || baseUrl.isBlank()) {
            Log.d(TAG, "API not configured, using offline responses")
            return generateResponse(query, intentResult, language)
        }

        return withContext(Dispatchers.IO) {
            try {
                callLlmApi(query, intentResult, language, apiKey, baseUrl)
            } catch (e: Exception) {
                Log.e(TAG, "LLM API call failed: ${e.message}, falling back to offline", e)
                generateResponse(query, intentResult, language)
            }
        }
    }

    private fun callLlmApi(
        query: String,
        intentResult: IntentResult,
        language: String,
        apiKey: String,
        baseUrl: String
    ): String {
        // Build system prompt
        val systemPrompt = buildSystemPrompt(intentResult, language)

        // Add user message to history
        conversationHistory.add(ChatMessage(role = "user", content = query))
        if (conversationHistory.size > MAX_HISTORY) {
            conversationHistory.removeAt(0)
        }

        // Build messages list (keep last 4 turns for speed)
        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage(role = "system", content = systemPrompt))
        val recentHistory = conversationHistory.takeLast(4)
        messages.addAll(recentHistory)

        // Try streaming first for faster perceived response
        return try {
            callLlmStreaming(messages, apiKey, baseUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Streaming failed, falling back to non-streaming: ${e.message}")
            callLlmNonStreaming(messages, apiKey, baseUrl)
        }
    }

    /**
     * Streaming API call - returns partial chunks via onStreamChunk callback
     * and accumulates the full response.
     */
    private fun callLlmStreaming(
        messages: List<ChatMessage>,
        apiKey: String,
        baseUrl: String
    ): String {
        val request = ChatRequest(messages = messages, stream = true)
        val jsonBody = gson.toJson(request)

        val cleanBase = baseUrl.trimEnd('/')
        val endpoint = "${cleanBase}/v1/chat/completions"

        Log.d(TAG, "Calling LLM API (streaming): $endpoint")

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val fullResponse = StringBuilder()
        val done = java.util.concurrent.CountDownLatch(1)
        val errorRef = java.util.concurrent.atomic.AtomicReference<Exception>(null)

        val factory = EventSources.createFactory(httpClient)
        factory.newEventSource(httpRequest, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    done.countDown()
                    return
                }
                try {
                    val jsonObj = gson.fromJson(data, java.util.Map::class.java)
                    val choices = jsonObj["choices"] as? List<*>
                    val firstChoice = choices?.firstOrNull() as? Map<*, *>
                    val delta = firstChoice?.get("delta") as? Map<*, *>
                    val content = delta?.get("content") as? String
                    if (!content.isNullOrBlank()) {
                        fullResponse.append(content)
                        onStreamChunk?.invoke(content)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing SSE chunk: ${e.message}")
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                val msg = t?.message ?: "HTTP ${response?.code}"
                errorRef.set(RuntimeException("Stream failed: $msg"))
                done.countDown()
            }

            override fun onClosed(eventSource: EventSource) {
                done.countDown()
            }
        })

        // Wait with timeout
        done.await(15, TimeUnit.SECONDS)
        val error = errorRef.get()
        if (error != null) throw error

        val reply = fullResponse.toString().trim()
        if (reply.isBlank()) throw RuntimeException("Empty streaming response")

        // Add to history
        conversationHistory.add(ChatMessage(role = "assistant", content = reply))
        if (conversationHistory.size > MAX_HISTORY) {
            conversationHistory.removeAt(0)
        }

        return reply
    }

    /**
     * Non-streaming fallback API call.
     */
    private fun callLlmNonStreaming(
        messages: List<ChatMessage>,
        apiKey: String,
        baseUrl: String
    ): String {
        val request = ChatRequest(messages = messages, stream = false)
        val jsonBody = gson.toJson(request)

        val cleanBase = baseUrl.trimEnd('/')
        val endpoint = "${cleanBase}/v1/chat/completions"

        Log.d(TAG, "Calling LLM API (non-streaming): $endpoint")

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            Log.e(TAG, "LLM API error ${response.code}: $responseBody")
            throw RuntimeException("API returned ${response.code}")
        }

        if (responseBody.isNullOrBlank()) {
            throw RuntimeException("Empty response from API")
        }

        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
        val reply = chatResponse.choices.firstOrNull()?.message?.content

        if (reply.isNullOrBlank()) {
            throw RuntimeException("No reply in API response")
        }

        // Add assistant reply to history
        conversationHistory.add(ChatMessage(role = "assistant", content = reply))
        if (conversationHistory.size > MAX_HISTORY) {
            conversationHistory.removeAt(0)
        }

        return reply.trim()
    }

    private fun buildSystemPrompt(intentResult: IntentResult, language: String): String {
        val isHindi = language.startsWith("hi")
        val intentName = intentResult.intent.name

        return if (isHindi) {
            """Tum TARZO ho — ek smart AI voice assistant jo Hindi/Hinglish mein baat karta hai.

Rules:
- Hamesha Hindi ya Hinglish mein jawab do
- Short aur helpful raho (2-3 sentences max)
- Agar koi command detect hui hai to pehle confirm karo phir bolna kya kar rahe ho
- Friendly aur confident tone rakho
- Detected intent: $intentName
- Agar koi general sawal hai to directly jawab do"""
        } else {
            """You are TARZO — a smart AI voice assistant.

Rules:
- Keep responses short and helpful (2-3 sentences max)
- If a command was detected, confirm what you're doing
- Be friendly and confident
- Detected intent: $intentName"""
        }
    }

    /**
     * Clear conversation history.
     */
    fun clearHistory() {
        conversationHistory.clear()
    }

    private fun generateDynamicResponse(
        query: String,
        intentResult: IntentResult,
        language: String
    ): String {
        return when (intentResult.intent) {
            IntentType.OPEN_APP -> {
                val app = intentResult.appName ?: "app"
                hinglish("$app khol raha hoon", language)
            }
            IntentType.CALL_CONTACT -> {
                val contact = intentResult.contactName ?: ""
                val phone = intentResult.phoneNumber
                when {
                    contact.isNotBlank() -> hinglish("$contact ko call kar raha hoon", language)
                    phone != null -> hinglish("$phone par call kar raha hoon", language)
                    else -> hinglish("Kaunse number ya contact par call karun?", language)
                }
            }
            IntentType.SEND_SMS -> {
                val contact = intentResult.contactName ?: ""
                val msg = intentResult.messageText ?: ""
                when {
                    contact.isNotBlank() && msg.isNotBlank() ->
                        hinglish("$contact ko message bhej raha hoon: $msg", language)
                    contact.isNotBlank() ->
                        hinglish("$contact ko kya message bhejun?", language)
                    else -> hinglish("Kaun ko aur kya message bhejun?", language)
                }
            }
            IntentType.OPEN_WHATSAPP -> {
                val contact = intentResult.contactName
                if (contact != null) {
                    hinglish("$contact ke liye WhatsApp khol raha hoon", language)
                } else {
                    hinglish("WhatsApp khol raha hoon", language)
                }
            }
            IntentType.YOUTUBE_SEARCH -> {
                val q = intentResult.queryText ?: ""
                if (q.isNotBlank()) {
                    hinglish("YouTube pe '$q' search kar raha hoon", language)
                } else {
                    hinglish("YouTube kya search karun?", language)
                }
            }
            IntentType.WEB_SEARCH -> {
                val q = intentResult.queryText ?: query
                if (q.isNotBlank()) {
                    hinglish("'$q' ke baare mein search kar raha hoon", language)
                } else {
                    hinglish("Kya search karna hai batao", language)
                }
            }
            IntentType.PLAY_MUSIC -> hinglish("Music chala raha hoon", language)
            IntentType.SET_ALARM -> {
                val time = intentResult.timeValue
                if (time != null) {
                    hinglish("$time baje ka alarm laga diya", language)
                } else {
                    hinglish("Kitne baje ka alarm lagana hai?", language)
                }
            }
            IntentType.SET_TIMER -> hinglish("Timer shuru kar diya", language)
            IntentType.SET_REMINDER -> hinglish("Reminder set kar diya", language)
            IntentType.TRANSLATE -> {
                val lang = intentResult.languageCode
                if (lang != null) {
                    hinglish("Translation shuru kar raha hoon", language)
                } else {
                    hinglish("Kis bhasha mein translate karna hai?", language)
                }
            }
            IntentType.REMEMBER -> {
                val q = intentResult.queryText
                if (!q.isNullOrBlank()) {
                    hinglish("Maine yaad rakh liya: $q", language)
                } else {
                    hinglish("Kya yaad rakhna hai?", language)
                }
            }
            IntentType.FORGET -> {
                val q = intentResult.queryText
                if (!q.isNullOrBlank()) {
                    hinglish("Bhul gaya: $q", language)
                } else {
                    hinglish("Kya bhulna hai?", language)
                }
            }
            IntentType.LIST_MEMORIES -> hinglish("Yeh sab kuch main yaad karta hoon:", language)
            IntentType.SCROLL_UP -> hinglish("Upar scroll kar raha hoon", language)
            IntentType.SCROLL_DOWN -> hinglish("Neeche scroll kar raha hoon", language)
            IntentType.ANALYZE_IMAGE -> hinglish("Image analyze kar raha hoon", language)
            IntentType.OCR_TEXT -> hinglish("Text read kar raha hoon", language)
            IntentType.UNKNOWN -> {
                val isHindi = language.startsWith("hi")
                if (isHindi) {
                    "Mujhe samajh nahi aaya. Kya aap dobara bata sakte hain?"
                } else {
                    "I didn't understand that. Could you please repeat?"
                }
            }
            else -> ""
        }
    }

    private fun hinglish(hindiResponse: String, language: String): String {
        return if (language.startsWith("en") && !language.contains("IN")) {
            translateHinglishToEnglish(hindiResponse)
        } else {
            hindiResponse
        }
    }

    private fun translateHinglishToEnglish(text: String): String {
        return text
            .replace("khol raha hoon", "opening")
            .replace("kar raha hoon", "processing")
            .replace("lag raha hoon", "setting")
            .replace("bhej raha hoon", "sending")
            .replace("chala raha hoon", "playing")
            .replace("search kar raha hoon", "searching for")
            .replace("yaad rakh liya", "I have remembered")
            .replace("bhul gaya", "I have forgotten")
            .replace("kya yaad", "I remember")
            .replace("set kar diya", "has been set")
            .replace("laga diya", "has been set")
            .replace("shuru kar diya", "started")
            .replace("ko call", "calling")
            .replace("ko message", "messaging")
            .replace("ke liye", "for")
            .replace("pe", "on")
            .replace("ke baare mein", "about")
    }

    companion object {
        private const val TAG = "LLMClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private data class ResponseKey(val intent: IntentType, val lang: String)

        private val responseMap = mapOf(
            ResponseKey(IntentType.FLASHLIGHT_ON, "hi-IN") to
                    "Flashlight chalu kar diya",
            ResponseKey(IntentType.FLASHLIGHT_ON, "en-IN") to
                    "Flashlight on kar diya",
            ResponseKey(IntentType.FLASHLIGHT_OFF, "hi-IN") to
                    "Flashlight band kar diya",
            ResponseKey(IntentType.FLASHLIGHT_OFF, "en-IN") to
                    "Flashlight off kar diya",
            ResponseKey(IntentType.BRIGHTNESS_UP, "hi-IN") to
                    "Brightness badha diya",
            ResponseKey(IntentType.BRIGHTNESS_DOWN, "hi-IN") to
                    "Brightness kam kar diya",
            ResponseKey(IntentType.VOLUME_UP, "hi-IN") to
                    "Volume badha diya",
            ResponseKey(IntentType.VOLUME_DOWN, "hi-IN") to
                    "Volume kam kar diya",
            ResponseKey(IntentType.WEATHER, "hi-IN") to
                    "Mausam ki jaankari de raha hoon",
            ResponseKey(IntentType.BATTERY_INFO, "hi-IN") to
                    "Battery ki jaankari de raha hoon",
            ResponseKey(IntentType.DATE_TIME, "hi-IN") to
                    "Main aapko waqt aur tarikh bata raha hoon",
            ResponseKey(IntentType.TAKE_PHOTO, "hi-IN") to
                    "Photo kheench raha hoon",
            ResponseKey(IntentType.TAKE_SELFIE, "hi-IN") to
                    "Selfie le raha hoon",
            ResponseKey(IntentType.RECORD_VIDEO, "hi-IN") to
                    "Video recording shuru kar raha hoon",
            ResponseKey(IntentType.ANTI_THEFT, "hi-IN") to
                    "Anti-theft mode activate kar diya. Aapka phone ab secure hai",
            ResponseKey(IntentType.WIFI_TOGGLE, "hi-IN") to
                    "WiFi toggle kar diya",
            ResponseKey(IntentType.BLUETOOTH_TOGGLE, "hi-IN") to
                    "Bluetooth toggle kar diya",
            ResponseKey(IntentType.PLAY_MUSIC, "hi-IN") to
                    "Music chala raha hoon",
            ResponseKey(IntentType.GO_BACK, "hi-IN") to
                    "Wapas gaya",
            ResponseKey(IntentType.GO_HOME, "hi-IN") to
                    "Home screen par gaya",
            ResponseKey(IntentType.GO_RECENTS, "hi-IN") to
                    "Recent apps khol diye",
            ResponseKey(IntentType.TYPE_TEXT, "hi-IN") to
                    "Likh diya",
            ResponseKey(IntentType.CLICK_ELEMENT, "hi-IN") to
                    "Click kar diya",
            ResponseKey(IntentType.LONG_PRESS_ELEMENT, "hi-IN") to
                    "Long press kar diya"
        )
    }
}