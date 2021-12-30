package io.github.toyota32k.ytremote.player

import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import io.github.toyota32k.ytremote.MainActivity
import io.github.toyota32k.ytremote.model.AppViewModel

class MediaSessionContainer(context:Context) : MediaSessionCompat.Callback() {
    val logger = MainActivity.logger
    val playerStateModel:PlayerStateModel by lazy {
        AppViewModel.instance.playerStateModel
    }
    val session = MediaSessionCompat(context,logger.tag).apply {
        setPlaybackState(PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .build())
        setCallback(this@MediaSessionContainer)
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        if(mediaButtonEvent!=null && mediaButtonEvent.getAction() == Intent.ACTION_MEDIA_BUTTON) {
            val event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? KeyEvent
            if (event != null) {
                val action = event.action
                if (action == KeyEvent.ACTION_DOWN) {
                    logger.debug(event.toString())
                    when(event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PAUSE,KeyEvent.KEYCODE_MEDIA_PLAY -> togglePlay()
                        KeyEvent.KEYCODE_MEDIA_NEXT -> playNext()
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> playPrev()
                    }
                }
            }
        }
        return super.onMediaButtonEvent(mediaButtonEvent)
    }

    private fun playPrev() {
        playerStateModel.commandNextVideo.invoke()
        playerStateModel.commandPlay.invoke()
    }

    private fun playNext() {
        playerStateModel.commandPrevVideo.invoke()
        playerStateModel.commandPlay.invoke()
    }

    private fun togglePlay() {
        playerStateModel.commandTogglePlay.invoke()
    }

    var active:Boolean
        get() = session.isActive
        set(v) { session.isActive = v }

    fun activate() {
        active = true
    }
}