package com.tarzo.ai.core.ai

import com.tarzo.ai.util.Constants
import com.tarzo.ai.util.Constants.IntentType
import com.tarzo.ai.util.Constants.TIME_PATTERN
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the extracted parameters from a voice command.
 */
data class IntentResult(
    val intent: IntentType,
    val confidence: Float,
    val contactName: String? = null,
    val appName: String? = null,
    val queryText: String? = null,
    val timeValue: String? = null,
    val phoneNumber: String? = null,
    val messageText: String? = null,
    val languageCode: String? = null,
    val amount: Int? = null
)

/**
 * Rule-based intent detection engine.
 * Parses user voice commands and maps them to structured intents with parameters.
 * Works completely offline — no API key needed.
 */
@Singleton
class IntentDetector @Inject constructor() {

    data class IntentRule(
        val intent: IntentType,
        val keywords: List<List<String>>, // OR of AND groups
        val extractors: List<ParamExtractor> = emptyList(),
        val baseConfidence: Float = 0.85f
    )

    interface ParamExtractor {
        fun extract(text: String, normalized: String): Pair<String, Any>?
    }

    private val rules: List<IntentRule> = buildRules()

    fun detectIntent(input: String): IntentResult {
        val normalized = normalize(input)
        val results = mutableListOf<Pair<IntentRule, Float>>()

        for (rule in rules) {
            val (matched, score) = matchRule(rule, normalized)
            if (matched) {
                results.add(rule to score)
            }
        }

        if (results.isEmpty()) {
            return IntentResult(intent = IntentType.WEB_SEARCH, confidence = 0.4f, queryText = input)
        }

        results.sortByDescending { it.second }
        val best = results.first()
        val rule = best.first
        val confidence = best.second

        val params = mutableMapOf<String, Any>()
        for (extractor in rule.extractors) {
            val result = extractor.extract(input, normalized)
            if (result != null) {
                params[result.first] = result.second
            }
        }

        return IntentResult(
            intent = rule.intent,
            confidence = confidence,
            contactName = params["contact"] as? String,
            appName = params["app"] as? String,
            queryText = params["query"] as? String ?: input,
            timeValue = params["time"] as? String,
            phoneNumber = params["phone"] as? String,
            messageText = params["message"] as? String,
            languageCode = params["language"] as? String
        )
    }

    private fun matchRule(rule: IntentRule, normalized: String): Pair<Boolean, Float> {
        for (keywordGroup in rule.keywords) {
            val allMatch = keywordGroup.all { keyword ->
                normalized.contains(keyword, ignoreCase = true)
            }
            if (allMatch) {
                val matchCount = keywordGroup.count { normalized.contains(it, ignoreCase = true) }
                val score = rule.baseConfidence * (matchCount.toFloat() / keywordGroup.size) + 0.1f
                return true to score.coerceAtMost(1.0f)
            }
        }
        return false to 0f
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[s]+"), " ")
            .trim()
            .replace("ऊ", "ू")
            .replace("ँ", "ं")
            .replace("ऐ", "ै")
            .replace("।", ".")
    }

    // ── Rule Definitions ─────────────────────────────────────────

    private fun buildRules(): List<IntentRule> {
        return listOf(
            // Flashlight
            IntentRule(
                IntentType.FLASHLIGHT_ON,
                keywords = listOf(
                    listOf("flashlight", "on"), listOf("torch", "on"),
                    listOf("roshni", "on"), listOf("light", "on"),
                    listOf("flashlight", "chalu"), listOf("torch", "chalu"),
                    listOf("light", "jala"), listOf("light", "chal"),
                    listOf("roshni", "chalu")
                )
            ),
            IntentRule(
                IntentType.FLASHLIGHT_OFF,
                keywords = listOf(
                    listOf("flashlight", "off"), listOf("torch", "off"),
                    listOf("roshni", "off"), listOf("light", "off"),
                    listOf("flashlight", "band"), listOf("torch", "band"),
                    listOf("light", "band"), listOf("light", "off"),
                    listOf("roshni", "band karo")
                )
            ),
            // Brightness
            IntentRule(
                IntentType.BRIGHTNESS_UP,
                keywords = listOf(
                    listOf("brightness", "up"), listOf("brightness", "badha"),
                    listOf("brightness", "increase"), listOf("screen", "bright"),
                    listOf("roshan", "badha"), listOf("brightness", "aage")
                )
            ),
            IntentRule(
                IntentType.BRIGHTNESS_DOWN,
                keywords = listOf(
                    listOf("brightness", "down"), listOf("brightness", "kam"),
                    listOf("brightness", "decrease"), listOf("screen", "dim"),
                    listOf("roshan", "kam")
                )
            ),
            // Volume
            IntentRule(
                IntentType.VOLUME_UP,
                keywords = listOf(
                    listOf("volume", "up"), listOf("volume", "badha"),
                    listOf("awaz", "badha"), listOf("sound", "badha"),
                    listOf("volume", "increase"), listOf("awaz", "chalu"),
                    listOf("loud")
                )
            ),
            IntentRule(
                IntentType.VOLUME_DOWN,
                keywords = listOf(
                    listOf("volume", "down"), listOf("volume", "kam"),
                    listOf("awaz", "kam"), listOf("sound", "kam"),
                    listOf("volume", "decrease"), listOf("quiet")
                )
            ),
            // Open App
            IntentRule(
                IntentType.OPEN_APP,
                keywords = listOf(
                    listOf("open", "app"), listOf("app", "kholo"),
                    listOf("launch", "app"), listOf("start", "app")
                ),
                extractors = listOf(AppNameExtractor())
            ),
            // Call
            IntentRule(
                IntentType.CALL_CONTACT,
                keywords = listOf(
                    listOf("call"), listOf("phone", "karo"),
                    listOf("dial"), listOf("ring", "karo"),
                    listOf("call", "karo"), listOf("phone", "lagao")
                ),
                extractors = listOf(ContactExtractor(), PhoneExtractor()),
                baseConfidence = 0.75f
            ),
            // SMS
            IntentRule(
                IntentType.SEND_SMS,
                keywords = listOf(
                    listOf("sms", "bhej"), listOf("message", "bhej"),
                    listOf("text", "bhej"), listOf("send", "sms"),
                    listOf("send", "message"), listOf("message", "karo")
                ),
                extractors = listOf(ContactExtractor(), MessageExtractor()),
                baseConfidence = 0.75f
            ),
            // WhatsApp
            IntentRule(
                IntentType.OPEN_WHATSAPP,
                keywords = listOf(
                    listOf("whatsapp", "kholo"), listOf("open", "whatsapp"),
                    listOf("whatsapp", "chalu"), listOf("launch", "whatsapp")
                ),
                extractors = listOf(ContactExtractor())
            ),
            // YouTube
            IntentRule(
                IntentType.YOUTUBE_SEARCH,
                keywords = listOf(
                    listOf("youtube", "search"), listOf("youtube", "pe", "search"),
                    listOf("youtube", "par"), listOf("youtube", "mein"),
                    listOf("search", "youtube"), listOf("play", "youtube")
                ),
                extractors = listOf(QueryExtractor())
            ),
            // Music
            IntentRule(
                IntentType.PLAY_MUSIC,
                keywords = listOf(
                    listOf("play", "music"), listOf("gaana", "chalu"),
                    listOf("music", "baja"), listOf("song", "chalu"),
                    listOf("gaana", "baja"), listOf("play", "song"),
                    listOf("music", "play")
                )
            ),
            // Web Search
            IntentRule(
                IntentType.WEB_SEARCH,
                keywords = listOf(
                    listOf("search"), listOf("khoj"),
                    listOf("google", "search"), listOf("search", "karo"),
                    listOf("dhundo"), listOf("find")
                ),
                extractors = listOf(QueryExtractor()),
                baseConfidence = 0.6f
            ),
            // Weather
            IntentRule(
                IntentType.WEATHER,
                keywords = listOf(
                    listOf("weather"), listOf("mausam"),
                    listOf("temperature"), listOf("tapman")
                )
            ),
            // Battery
            IntentRule(
                IntentType.BATTERY_INFO,
                keywords = listOf(
                    listOf("battery"), listOf("charge"),
                    listOf("battery", "kitna"), listOf("charge", "kitna"),
                    listOf("battery", "bacha"), listOf("battery", "status")
                )
            ),
            // Date/Time
            IntentRule(
                IntentType.DATE_TIME,
                keywords = listOf(
                    listOf("time", "kya"), listOf("date", "kya"),
                    listOf("time", "bata"), listOf("date", "bata"),
                    listOf("waqt"), listOf("tarikh"),
                    listOf("kitne", "baje"), listOf("aaj", "kya", "date")
                )
            ),
            // Alarm
            IntentRule(
                IntentType.SET_ALARM,
                keywords = listOf(
                    listOf("alarm", "lagao"), listOf("alarm", "set"),
                    listOf("alarm", "rakh"), listOf("set", "alarm"),
                    listOf("jaga", "do")
                ),
                extractors = listOf(TimeExtractor())
            ),
            // Timer
            IntentRule(
                IntentType.SET_TIMER,
                keywords = listOf(
                    listOf("timer", "lagao"), listOf("timer", "set"),
                    listOf("timer", "chalu"), listOf("set", "timer")
                )
            ),
            // Reminder
            IntentRule(
                IntentType.SET_REMINDER,
                keywords = listOf(
                    listOf("reminder", "lagao"), listOf("yaad", "rakh"),
                    listOf("reminder", "set"), listOf("yaad", "dilwa"),
                    listOf("set", "reminder"), listOf("niram", "rakho")
                )
            ),
            // Photo
            IntentRule(
                IntentType.TAKE_PHOTO,
                keywords = listOf(
                    listOf("photo", "kheencho"), listOf("picture", "le"),
                    listOf("camera", "kholo"), listOf("photo", "le"),
                    listOf("selfie", "nhi"), listOf("tasveer", "kheencho")
                )
            ),
            // Selfie
            IntentRule(
                IntentType.TAKE_SELFIE,
                keywords = listOf(
                    listOf("selfie", "le"), listOf("selfie", "kheencho"),
                    listOf("front", "camera"), listOf("selfie", "bana")
                )
            ),
            // Video
            IntentRule(
                IntentType.RECORD_VIDEO,
                keywords = listOf(
                    listOf("video", "ban"), listOf("record", "karo"),
                    listOf("video", "record"), listOf("video", "shuru")
                )
            ),
            // Scroll
            IntentRule(
                IntentType.SCROLL_UP,
                keywords = listOf(
                    listOf("upar", "scroll"), listOf("scroll", "up"),
                    listOf("upar", "jao")
                )
            ),
            IntentRule(
                IntentType.SCROLL_DOWN,
                keywords = listOf(
                    listOf("neeche", "scroll"), listOf("scroll", "down"),
                    listOf("neeche", "jao")
                )
            ),
            // Image analysis
            IntentRule(
                IntentType.ANALYZE_IMAGE,
                keywords = listOf(
                    listOf("image", "analyze"), listOf("photo", "analyze"),
                    listOf("picture", "describe"), listOf("ye", "kya", "hai"),
                    listOf("image", "samjhao")
                ),
                baseConfidence = 0.5f
            ),
            // OCR
            IntentRule(
                IntentType.OCR_TEXT,
                keywords = listOf(
                    listOf("text", "read"), listOf("ocr"),
                    listOf("shabd", "padho"), listOf("likha", "kya", "hai"),
                    listOf("text", "nikalo")
                )
            ),
            // Translate
            IntentRule(
                IntentType.TRANSLATE,
                keywords = listOf(
                    listOf("translate"), listOf("anuvad"),
                    listOf("translation"), listOf("bhasha", "badlo")
                ),
                extractors = listOf(LanguageExtractor())
            ),
            // Remember
            IntentRule(
                IntentType.REMEMBER,
                keywords = listOf(
                    listOf("yaad", "rakh"), listOf("remember"),
                    listOf("note", "kar"), listOf("save", "kar")
                ),
                extractors = listOf(QueryExtractor()),
                baseConfidence = 0.7f
            ),
            // Forget
            IntentRule(
                IntentType.FORGET,
                keywords = listOf(
                    listOf("bhul", "jao"), listOf("forget"),
                    listOf("delete", "memory"), listOf("hata", "de"),
                    listOf("yaad", "se", "hata")
                ),
                extractors = listOf(QueryExtractor()),
                baseConfidence = 0.7f
            ),
            // List Memories
            IntentRule(
                IntentType.LIST_MEMORIES,
                keywords = listOf(
                    listOf("yaad", "kya"), listOf("memory", "list"),
                    listOf("kya", "yaad"), listOf("sab", "yaad"),
                    listOf("what", "do", "you", "remember")
                )
            ),
            // Anti-theft
            IntentRule(
                IntentType.ANTI_THEFT,
                keywords = listOf(
                    listOf("anti", "theft"), listOf("chori", "se"),
                    listOf("secure", "phone"), listOf("phone", "lock")
                )
            ),
            // WiFi
            IntentRule(
                IntentType.WIFI_TOGGLE,
                keywords = listOf(
                    listOf("wifi", "on"), listOf("wifi", "off"),
                    listOf("wifi", "chalu"), listOf("wifi", "band"),
                    listOf("wifi", "toggle")
                )
            ),
            // Bluetooth
            IntentRule(
                IntentType.BLUETOOTH_TOGGLE,
                keywords = listOf(
                    listOf("bluetooth", "on"), listOf("bluetooth", "off"),
                    listOf("bluetooth", "chalu"), listOf("bluetooth", "band"),
                    listOf("bluetooth", "toggle")
                )
            )
        )
    }

    // ── Parameter Extractors ─────────────────────────────────────

    private class ContactExtractor : ParamExtractor {
        override fun extract(text: String, normalized: String): Pair<String, Any>? {
            val patterns = listOf(
                Regex("(?:ko|ke|se|ka|ki|ke liye)+(.+?)$", RegexOption.IGNORE_CASE),
                Regex("(?:to|for)+(.+?)$", RegexOption.IGNORE_CASE),
                Regex("(?:call|phone|dial|ring|sms|message|whatsapp)+(.+?)$", RegexOption.IGNORE_CASE)
            )
            for (pattern in patterns) {
                val match = pattern.find(normalized)
                if (match != null) {
                    val name = match.groupValues[1].trim()
                        .replace(Regex("(bhejo|karo|lagao|pe|par|mein|se|ko|ka|ki)"), "")
                        .trim()
                    if (name.length in 2..30 && !name.contains(Regex("d{4,}"))) {
                        return "contact" to name
                    }
                }
            }
            return null
        }
    }

    private class PhoneExtractor : ParamExtractor {
        override fun extract(text: String, normalized: String): Pair<String, Any>? {
            val phoneMatch = Regex("bd{10}b").find(text)
            if (phoneMatch != null) {
                return "phone" to phoneMatch.value
            }
            val phoneMatch2 = Regex("b+?[91]{0,2}[s-]?d{5}[s-]?d{5}b").find(text)
            if (phoneMatch2 != null) {
                return "phone" to phoneMatch2.value.replace(Regex("[^d]"), "")
            }
            return null
        }
    }

    private class AppNameExtractor : ParamExtractor {
        override fun extract(text: String, normalized: String): Pair<String, Any>? {
            val commonApps = mapOf(
                "whatsapp" to "WhatsApp", "instagram" to "Instagram",
                "facebook" to "Facebook", "twitter" to "Twitter",
                "x" to "X", "youtube" to "YouTube",
                "chrome" to "Chrome", "firefox" to "Firefox",
                "camera" to "Camera", "gallery" to "Gallery",
                "photos" to "Photos", "settings" to "Settings",
                "calculator" to "Calculator", "calendar" to "Calendar",
                "clock" to "Clock", "maps" to "Maps",
                "google maps" to "Google Maps", "gmail" to "Gmail",
                "mail" to "Gmail", "telegram" to "Telegram",
                "snapchat" to "Snapchat", "linkedin" to "LinkedIn",
                "spotify" to "Spotify", "netflix" to "Netflix",
                "amazon" to "Amazon", "paytm" to "Paytm",
                "phonepe" to "PhonePe", "gpay" to "Google Pay",
                "google pay" to "Google Pay", "swiggy" to "Swiggy",
                "zomato" to "Zomato", "uber" to "Uber",
                "ola" to "Ola", "flipkart" to "Flipkart",
                "notepad" to "Notes", "files" to "Files"
            )
            for ((key, value) in commonApps) {
                if (normalized.contains(key)) {
                    return "app" to value
                }
            }
            val openMatch = Regex("(?:open|launch|start|kholo|chalu)+(.+?)$", RegexOption.IGNORE_CASE)
            val match = openMatch.find(normalized)
            if (match != null) {
                val appName = match.groupValues[1].trim()
                    .split(" ").first()
                    .replaceFirstChar { it.uppercase() }
                if (appName.length in 2..20) {
                    return "app" to appName
                }
            }
            return null
        }
    }

    private class QueryExtractor : ParamExtractor {
        override fun extract(text: String, normalized: String): Pair<String, Any>? {
            val stopWords = setOf(
                "search", "karo", "khoj", "dhundo", "find", "google",
                "youtube", "pe", "par", "mein", "me", "bata", "batao",
                "what", "is", "the", "kya", "hai", "hota", "how",
                "why", "when", "where", "who", "which"
            )
            val words = normalized.split(" ")
                .filter { it.length > 1 && it !in stopWords }
            if (words.size >= 2) {
                return "query" to words.joinToString(" ")
            }
            if (words.size == 1) {
                return "query" to words.first()
            }
            return null
        }
    }

    private class TimeExtractor : ParamExtractor {
        override fun extract(text: String, normalized: String): Pair<String, Any>? {
            val relativeTime = when {
                Regex("(?:subah|morning)").containsMatchIn(normalized) -> "07:00"
                Regex("(?:dopahar|afternoon)").containsMatchIn(normalized) -> "13:00"
                Regex("(?:shaam|evening)").containsMatchIn(normalized) -> "17:00"
                Regex("(?:raat|night)").containsMatchIn(normalized) -> "21:00"
                else -> null
            }
            if (relativeTime != null) return "time" to relativeTime

            val match = TIME_PATTERN.find(normalized)
            if (match != null) {
                val hour = match.groupValues[1].padStart(2, '0')
                val minute = match.groupValues[2].ifEmpty { "00" }
                val ampm = match.groupValues[3].lowercase()
                var h = hour.toInt()
                if (ampm == "pm" && h < 12) h += 12
                if (ampm == "am" && h == 12) h = 0
                return "time" to "${h.toString().padStart(2, '0')}:$minute"
            }
            return null
        }
    }

    private class MessageExtractor : ParamExtractor {
        override fun extract(text: String, normalized: String): Pair<String, Any>? {
            val patterns = listOf(
                Regex("""(?:message|sms|text)+(?:karo|bhej|send)+[^]*?(?:ki|that|saying|message)+[](.+?)[]"""),
                Regex("""(?:message|sms|text)+(?:karo|bhej|send)+[^]*?+(?:ki|that|saying|message)+(.+?)$""", RegexOption.IGNORE_CASE),
                Regex("""(?:bhej|send)+[^]*?[](.+?)[]""")
            )
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val msg = match.groupValues[1].trim()
                    if (msg.length in 2..500) {
                        return "message" to msg
                    }
                }
            }
            val kiMatch = Regex("(?:ki|that)+(.+?)$", RegexOption.IGNORE_CASE).find(normalized)
            if (kiMatch != null) {
                val msg = kiMatch.groupValues[1].trim()
                if (msg.length in 2..200 && !msg.contains(Regex("d{10}"))) {
                    return "message" to msg
                }
            }
            return null
        }
    }

    private class LanguageExtractor : ParamExtractor {
        override fun extract(text: String, normalized: String): Pair<String, Any>? {
            val languages = mapOf(
                "hindi" to "hi", "english" to "en",
                "urdu" to "ur", "tamil" to "ta",
                "telugu" to "te", "bengali" to "bn",
                "marathi" to "mr", "gujarati" to "gu",
                "kannada" to "kn", "malayalam" to "ml",
                "punjabi" to "pa", "french" to "fr",
                "spanish" to "es", "german" to "de",
                "japanese" to "ja", "chinese" to "zh",
                "arabic" to "ar", "korean" to "ko",
                "russian" to "ru", "portuguese" to "pt"
            )
            for ((name, code) in languages) {
                if (normalized.contains(name)) {
                    return "language" to code
                }
            }
            return null
        }
    }
}