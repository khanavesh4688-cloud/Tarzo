package com.tarzo.ai.core.ai

import com.tarzo.ai.util.Constants.IntentType
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/*
 * ──────────────────────────────────────────────────────────────
 *  HOW TO PLUG IN A REAL LLM (OpenAI / Gemini) BACKEND:
 * ──────────────────────────────────────────────────────────────
 *
 *  1. Set up a backend server (Node.js / Python Flask / FastAPI)
 *     that holds your API key securely (NEVER embed API keys in
 *     the Android APK).
 *
 *  2. Define an endpoint, e.g. POST /api/v1/chat
 *     Body: { "query": "...", "intent": "FLASHLIGHT_ON", "lang": "hi-IN" }
 *
 *  3. In this LLMClient, replace generateResponse() with an
 *     OkHttp / Retrofit call to your backend.
 *
 *  Example (Retrofit):
 *  ─────────────────────
 *  @POST("/api/v1/chat")
 *  suspend fun chat(@Body req: ChatRequest): ChatResponse
 *
 *  // In generateResponse():
 *  val resp = api.chat(ChatRequest(query, intent.intent.name, lang))
 *  return resp.reply
 *
 *  4. For OpenAI:
 *     Backend sends: { model: "gpt-4o-mini", messages: [...] }
 *     to https://api.openai.com/v1/chat/completions
 *
 *  5. For Gemini:
 *     Backend sends to:
 *     https://generativelanguage.googleapis.com/v1beta/models/
 *     gemini-2.0-flash:generateContent?key=YOUR_KEY
 *
 *  The default implementation below works fully offline with
 *  pre-built Hinglish rule-based responses.
 * ──────────────────────────────────────────────────────────────
 */

/**
 * Generates natural-language responses for detected intents.
 * Default implementation is rule-based and works offline.
 * Can be swapped with an LLM API backend.
 */
@Singleton
class LLMClient @Inject constructor() {

    fun generateResponse(
        query: String,
        intentResult: IntentResult,
        language: String = "hi-IN"
    ): String {
        val responseKey = ResponseKey(intentResult.intent, language)
        return responseMap[responseKey]
            ?: generateDynamicResponse(query, intentResult, language)
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
                    "Music chala raha hoon"
        )
    }
}
