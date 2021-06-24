package io.github.toyota32k.ytremote.player

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import com.michael.ytremote.view.ChapterView
import io.github.toyota32k.bindit.Binder
import io.github.toyota32k.utils.*
import io.github.toyota32k.ytremote.R
import io.github.toyota32k.ytremote.model.AppViewModel
import io.github.toyota32k.ytremote.utils.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
class MicVideoPlayer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        val supportPinP: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    private val mHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    var binder: MicBinder

    inner class MicBinder : Binder() {
        val exoPlayerView: PlayerView = findViewById<PlayerView>(R.id.exp_playerView).apply {
            useController = true /* ExoPlayerのControllerを表示する */
        }
        private val progressRingManager = ProgressRingManager(findViewById(R.id.exp_progressRing))

        fun bindPlayer(player: SimpleExoPlayer, enableFullscreen: Boolean, enablePinP: Boolean, enableClose:Boolean) {
            reset()
            exoPlayerView.player = player
            val owner = lifecycleOwner()!!
            val appViewModel = AppViewModel.instance
            register(
                appViewModel.playerStateModel.commandPrevVideo.connectViewEx(
                    exoPlayerView.findViewById<View>(
                        R.id.mic_ctr_exo_prev
                    ).apply { visibility = VISIBLE }
                ),
                appViewModel.playerStateModel.commandNextVideo.connectViewEx(
                    exoPlayerView.findViewById<View>(
                        R.id.mic_ctr_exo_next
                    ).apply { visibility = VISIBLE }
                ),
                appViewModel.playerStateModel.commandPrevChapter.connectViewEx(
                    exoPlayerView.findViewById<View>(
                        R.id.mic_ctr_exo_prev_chapter
                    ).apply { visibility = VISIBLE }
                ),
                appViewModel.playerStateModel.commandNextChapter.connectViewEx(
                    exoPlayerView.findViewById<View>(
                        R.id.mic_ctr_exo_next_chapter
                    ).apply { visibility = VISIBLE }
                ),
                appViewModel.playerStateModel.commandTogglePlay.connectViewEx(this@MicVideoPlayer),
                appViewModel.playerStateModel.chapterSource.chapterInfo.disposableObserve(owner) {
                    exoPlayerView.findViewById<ChapterView>(
                        R.id.mic_chapter_view
                    )?.setChapterList(it)
                },
                appViewModel.playerStateModel.playerState.disposableObserve(owner) { if (it == PlayerStateModel.PlayerState.Loading) progressRingManager.show() else progressRingManager.hide() },
                appViewModel.playerStateModel.videoSize.disposableObserve(owner) {
                    onVideoSizeChanged(
                        it ?: return@disposableObserve
                    )
                }
            )
            exoPlayerView.findViewById<View>(R.id.mic_ctr_full_button).apply {
                visibility = if (enableFullscreen) {
                    register(appViewModel.playerStateModel.commandFullscreen.connectViewEx(this))
                    VISIBLE
                } else GONE
            }
            exoPlayerView.findViewById<View>(R.id.mic_ctr_pinp_button).apply {
                visibility = if (enablePinP && supportPinP) {
                    register(appViewModel.playerStateModel.commandPinP.connectViewEx(this))
                    VISIBLE
                } else GONE
            }
            exoPlayerView.findViewById<View>(R.id.mic_ctr_close_button).apply {
                visibility = if(enableClose) {
                    register(appViewModel.playerStateModel.commandCloseFullscreen.connectViewEx(this))
                    VISIBLE
                } else GONE
            }
        }

        fun unbindPlayer() {
            reset()
            exoPlayerView.player = null
        }

        override fun dispose() {
            super.dispose()
            unbindPlayer()
        }

        private fun Size.isEmpty(): Boolean {
            return width == 0 && height == 0
        }

        private val fitter = Fitter()
        private var cachedVideoSize: Size? = null
        private fun onVideoSizeChanged(videoSize: Size) {
            if (videoSize.isEmpty()) return
            cachedVideoSize = videoSize
            val playerSize = fitter.fit(videoSize.width, videoSize.height)
            exoPlayerView.setLayoutSize(playerSize.width, playerSize.height)
        }

        fun onViewSizeChanged(width: Int, height: Int) {
            if (fitter.setHint(FitMode.Inside, width, height)) {
                onVideoSizeChanged(cachedVideoSize ?: return)
            }
        }
    }

    // endregion

    // region Initialization / Termination

    init {
        LayoutInflater.from(context).inflate(R.layout.video_exo_player, this)
        isSaveFromParentEnabled = false
        binder = MicBinder()
    }

    override fun onDetachedFromWindow() {
        binder.unbindPlayer()
        super.onDetachedFromWindow()
    }

    var showDefaultController:Boolean
        get() = binder.exoPlayerView.useController
        set(v) { binder.exoPlayerView.useController = v }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            mHandler.postDelayed({ binder.onViewSizeChanged(w, h) }, 200)
        }
    }

    fun bindPlayer(player: SimpleExoPlayer, enableFullscreen: Boolean, enablePinP: Boolean, enableClose: Boolean) {
        binder.bindPlayer(player, enableFullscreen, enablePinP, enableClose)
    }
    fun unbindPlayer() {
        binder.unbindPlayer()
    }

    // for PinP
    fun play() {
        binder.exoPlayerView.player?.play()
    }
    fun pause() {
        binder.exoPlayerView.player?.pause()
    }
    fun seekStart() {
        binder.exoPlayerView.player?.seekTo(0L)
    }
}