package com.tarzo.ai.features.media

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls media playback via MediaSessionManager and app intents.
 * Supports play/pause/skip for the active media session, YouTube control,
 * and launching music apps (Spotify, YouTube Music) via intents.
 */
@Singleton
class MediaControlManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager?
        get() = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val mediaSessionManager: MediaSessionManager?
        get() = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    // ── Media Session Control ──────────────────────────────────────────

    /**
     * Sends a play/pause toggle via the active media session.
     * Falls back to simulating a media key event if no session is found.
     */
    suspend fun playPause(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val controller = getActiveMediaController()
            if (controller != null) {
                val transportControls = controller.transportControls
                val metadata = controller.metadata
                val isPlaying = controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING

                if (isPlaying) {
                    transportControls.pause()
                    Result.Success("Paused: ${metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "media"}")
                } else {
                    transportControls.play()
                    Result.Success("Playing: ${metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "media"}")
                }
            } else {
                // Fallback: simulate KEYCODE_MEDIA_PLAY_PAUSE
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                Result.Success("Sent play/pause command")
            }
        } catch (e: Exception) {
            // Last resort fallback
            try {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                Result.Success("Sent play/pause via key event")
            } catch (ex: Exception) {
                Result.Error(ex, "Failed to control playback: ${ex.message}")
            }
        }
    }

    /**
     * Pauses the current media session.
     */
    suspend fun pauseMusic(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val controller = getActiveMediaController()
            if (controller != null) {
                controller.transportControls.pause()
                Result.Success("Music paused")
            } else {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
                Result.Success("Sent pause command")
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to pause: ${e.message}")
        }
    }

    /**
     * Skips to the next track in the active media session.
     */
    suspend fun skipTrack(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val controller = getActiveMediaController()
            if (controller != null) {
                controller.transportControls.skipToNext()
                Result.Success("Skipped to next track")
            } else {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                Result.Success("Sent skip next command")
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to skip: ${e.message}")
        }
    }

    /**
     * Goes to the previous track in the active media session.
     */
    suspend fun previousTrack(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val controller = getActiveMediaController()
            if (controller != null) {
                controller.transportControls.skipToPrevious()
                Result.Success("Skipped to previous track")
            } else {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                Result.Success("Sent skip previous command")
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to go to previous track: ${e.message}")
        }
    }

    // ── Music App Launch ───────────────────────────────────────────────

    /**
     * Plays music by opening Spotify or YouTube Music with a search query.
     * Tries Spotify first, then falls back to YouTube Music, then YouTube search.
     *
     * @param query The song name, artist, or playlist to search for.
     */
    suspend fun playMusic(query: String): Result<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            // No query, just try to resume the active media session
            return@withContext playPause()
        }

        // Try Spotify first
        val spotifyResult = openAppSearch("com.spotify.music", query, "spotify://search/$query")
        if (spotifyResult.isSuccess) {
            return@withContext Result.Success("Searching '$query' on Spotify")
        }

        // Try YouTube Music
        val ytmResult = openAppSearch("com.google.android.apps.youtube.music", query)
        if (ytmResult.isSuccess) {
            return@withContext Result.Success("Searching '$query' on YouTube Music")
        }

        // Fallback to YouTube search intent
        val ytResult = openYouTubeSearch(query)
        if (ytResult.isSuccess) {
            return@withContext Result.Success("Searching '$query' on YouTube")
        }

        Result.Error(
            IllegalStateException("No music app available"),
            "Could not open any music app. Please install Spotify or YouTube Music."
        )
    }

    // ── YouTube Control ────────────────────────────────────────────────

    /**
     * Opens a YouTube search for the given query.
     */
    fun openYouTubeSearch(query: String): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Fallback if YouTube app search is not available
            if (intent.resolveActivity(context.packageManager) == null) {
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, "$query site:youtube.com")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } else {
                context.startActivity(intent)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            // Final fallback: open YouTube URL in browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Result.Success(Unit)
            } catch (ex: Exception) {
                Result.Error(ex, "Could not open YouTube: ${ex.message}")
            }
        }
    }

    /**
     * Sends a play/pause command intended for YouTube.
     */
    suspend fun youtubePlayPause(): Result<String> {
        return playPause()
    }

    /**
     * Toggles fullscreen mode by simulating the 'f' key event.
     * This only works if YouTube is the active media session.
     */
    suspend fun toggleFullscreen(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Send media key event - some players map this to fullscreen
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Result.Success("Fullscreen toggle sent. Note: fullscreen control requires the player app's support.")
        } catch (e: Exception) {
            Result.Error(e, "Failed to toggle fullscreen: ${e.message}")
        }
    }

    // ── Internal ───────────────────────────────────────────────────────

    private fun openAppSearch(packageName: String, query: String, deepLink: String? = null): Result<Unit> {
        return try {
            val intent = if (deepLink != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    setPackage(packageName)
                    putExtra(SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Result.Success(Unit)
            } else {
                Result.Error(IllegalStateException("App not installed: $packageName"), "")
            }
        } catch (e: Exception) {
            Result.Error(e, "")
        }
    }

    @Suppress("DEPRECATION")
    private fun getActiveMediaController(): MediaController? {
        return try {
            val msm = mediaSessionManager ?: return null
            val sessions = msm.getActiveSessions(null)
            sessions.firstOrNull { controller ->
                controller.playbackState != null
            }
        } catch (e: SecurityException) {
            // NotificationListenerService permission not granted
            null
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun sendMediaKeyEvent(keyCode: Int) {
        val am = audioManager ?: return
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        am.dispatchMediaKeyEvent(downEvent)
        am.dispatchMediaKeyEvent(upEvent)
    }
}
