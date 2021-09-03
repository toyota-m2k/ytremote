package io.github.toyota32k.ytremote.player

import android.content.Context
import android.util.Size
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.github.toyota32k.utils.SuspendableEvent
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.ytremote.BooApplication
import io.github.toyota32k.ytremote.R
import io.github.toyota32k.ytremote.model.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerModelBridge(val appViewModel: AppViewModel, val stateModel:PlayerStateModel) {
    companion object {
        val logger = UtLog("Exo", omissionNamespace = "io.github.toyota32k.ytremote")
    }
    lateinit var context:Context
    var player: SimpleExoPlayer? = null
        private set
    var loading:Boolean = false
    var playing:Boolean
        get() = stateModel.isPlaying.value == true
        set(v) { stateModel.isPlaying.value = v }
    private var watching = false
    private var needToWatch = SuspendableEvent(signal = false, autoReset = false)
    private var disabledRanges:List<Range>? = null
    private val mediaSourceFactory = ProgressiveMediaSource.Factory(        // ExtractorMediaSource ... non-adaptiveなほとんどのファイルに対応
        DefaultDataSourceFactory(BooApplication.instance, "amv")
    )

    init {
        stateModel.currentItem.observeForever { item->
            if(item!=null) {
                stateModel.onReset()
                player?.apply {
                    setMediaSource(mediaSourceFactory.createMediaSource(MediaItem.fromUri(item.url)), true)
                    prepare()
                    play()
                }

            }
        }
        stateModel.chapterSource.chapterInfo.observeForever {
            if(it!=null) {
                disabledRanges = it.disabledRanges
                watchStart()
            } else {
                disabledRanges = null
                watchStop()
            }
        }
        stateModel.isPinP.observeForever {
            if(it==true) {
                pinpUpdate()
            }
        }

        stateModel.commandPrevVideo.bindForever {
            appViewModel.prevVideo()
        }
        stateModel.commandNextVideo.bindForever {
            appViewModel.nextVideo()
        }
        stateModel.commandPrevChapter.bindForever {
            val chapterList = stateModel.chapterSource.chapterInfo.value?.list ?: return@bindForever
            val player = this.player ?: return@bindForever
            val c = chapterList.prev(player.currentPosition) ?: return@bindForever
            player.seekTo(c.position)
            if(c.label.isNotBlank()) {
                stateModel.chapterSelected.invoke(c.label)
            }
        }
        stateModel.commandNextChapter.bindForever {
            val chapterList = stateModel.chapterSource.chapterInfo.value?.list ?: return@bindForever
            val player = this.player ?: return@bindForever
            val c = chapterList.next(player.currentPosition) ?: return@bindForever
            player.seekTo(c.position)
            if(c.label.isNotBlank()) {
                stateModel.chapterSelected.invoke(c.label)
            }
        }
        stateModel.commandTogglePlay.bindForever {
            this.player?.apply {
                if(isPlaying) {
                    play()
                } else {
                    pause()
                }
            }
        }
    }

    private fun onPlayingChanged(isPlaying:Boolean) {
        if(isPlaying) {
            playing = true
            stateModel.onPlay()
            watchStart()
        } else {
            playing = false
            stateModel.onPause()
            watchStop()
        }
        pinpUpdate()
    }

    private fun pinpUpdate() {
        stateModel.updateButtonOnPinP.invoke(playing)
    }

    private val mVideoListener = object : Player.Listener {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            stateModel.videoSize.value = Size(width,height)
        }

        override fun onRenderedFirstFrame() {

        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            // speed, pitch, skipSilence, scaledUsPerMs
        }

        override fun onPlayerError(error: PlaybackException) {
            logger.stackTrace(error)
            stateModel.onError(context.getString(R.string.error))
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            logger.debug("loading = $isLoading")
            loading = isLoading
            if(isLoading) {
                stateModel.onLoading()
            }
        }

        override fun onPositionDiscontinuity(reason: Int) {
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        }

//        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
//            logger.info("playWhenReady = $playWhenReady")
//        }

        override fun onPlaybackStateChanged(playbackState: Int) {

            val ppn = {s:Int->
                when(s) {
                    Player.STATE_IDLE -> "Idle"
                    Player.STATE_BUFFERING -> "Buffering"
                    Player.STATE_READY -> "Ready"
                    Player.STATE_ENDED -> "Ended"
                    else -> "Unknown"
                }
            }

            logger.debug("status = ${ppn(playbackState)}")

            when(playbackState) {
                Player.STATE_READY -> {
                    stateModel.onLoaded(player?.duration?:0, player?.playWhenReady?:false)
                    val lpi = appViewModel.lastPlayInfo ?: return
                    appViewModel.lastPlayInfo = null
                    if(lpi.id == appViewModel.currentItem.value?.id) {
                        player?.seekTo(lpi.position)
                        if(!lpi.playing) {
                            player?.pause()
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    stateModel.onEnd()
                }
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerModelBridge.onPlayingChanged(isPlaying)
        }
    }

    private fun watchControl(sw:Boolean) {
        appViewModel.viewModelScope.launch {
            if(!sw || !playing || disabledRanges?.size?:0 ==0 || player ==null ) {
                if(!watching) {
                    watching = true
                    watchLoop()
                }
                needToWatch.reset()
            } else {
                needToWatch.set()
            }
        }
    }
    private fun watchStart() = watchControl(true)
    private fun watchStop() = watchControl(false)

    private fun watchLoop() {

        appViewModel.viewModelScope.launch {
            while(true) {
                needToWatch.withLock {
                    val dr = disabledRanges ?: return@withLock
                    val player = this@PlayerModelBridge.player ?: return@withLock
                    if(dr.isEmpty()) return@withLock

                    val pos = player.currentPosition
                    val hit = dr.firstOrNull { it.contains(pos) }
                    if (hit != null) {
                        if (hit.end == 0L || hit.end >= stateModel.duration.value!!) {
                            stateModel.onEnd()
                        } else {
                            player.seekTo(hit.end)
                        }
                    }
                }
                delay(200)
            }
        }
    }

    fun preparePlayer(context: Context) {
        this.context = context
        if(null==player) {
            player = SimpleExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                addListener(mVideoListener)
            }
        }
    }

    fun closePlayer() {
        player?.release()
        player = null
    }
}