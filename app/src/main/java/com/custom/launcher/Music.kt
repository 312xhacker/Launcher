package com.custom.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** Empty service: its only job is to let the user grant "notification access", which unlocks media sessions. */
class MediaNotificationListener : NotificationListenerService()

data class Track(val title: String, val artist: String, val art: ImageBitmap?, val playing: Boolean, val duration: Long)

class MusicController(private val ctx: Context) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val cn = ComponentName(ctx, MediaNotificationListener::class.java)
    val track = MutableStateFlow<Track?>(null)
    val hasAccess = MutableStateFlow(false)
    private var controller: MediaController? = null
    private var registered = false

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
    }
    private val sl = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    fun connect() {
        val ok = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        hasAccess.value = ok
        if (!ok) return
        runCatching {
            if (!registered) { msm.addOnActiveSessionsChangedListener(sl, cn, Handler(Looper.getMainLooper())); registered = true }
            pick(msm.getActiveSessions(cn))
        }
    }

    private fun pick(list: List<MediaController>?) {
        controller?.unregisterCallback(cb)
        controller = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        controller?.registerCallback(cb)
        update()
    }

    private fun update() {
        val c = controller; val m = c?.metadata
        if (c == null || m == null) { track.value = null; return }
        val art = (m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART))?.asImageBitmap()
        track.value = Track(
            m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
            m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            art, c.playbackState?.state == PlaybackState.STATE_PLAYING,
            m.getLong(MediaMetadata.METADATA_KEY_DURATION))
    }

    fun position(): Long {
        val s = controller?.playbackState ?: return 0
        if (s.state == PlaybackState.STATE_PLAYING) {
            val dt = SystemClock.elapsedRealtime() - s.lastPositionUpdateTime
            return s.position + (dt * s.playbackSpeed).toLong()
        }
        return s.position
    }

    fun toggle() { val t = controller?.transportControls ?: return; if (track.value?.playing == true) t.pause() else t.play() }
    fun next() { controller?.transportControls?.skipToNext() }
    fun prev() { controller?.transportControls?.skipToPrevious() }
    fun openAccessSettings() {
        ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun release() { if (registered) msm.removeOnActiveSessionsChangedListener(sl); controller?.unregisterCallback(cb) }
}
