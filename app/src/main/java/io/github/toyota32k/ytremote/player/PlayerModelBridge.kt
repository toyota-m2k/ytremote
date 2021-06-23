package io.github.toyota32k.ytremote.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.github.toyota32k.utils.SuspendableEvent
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.UtLogger
import io.github.toyota32k.ytremote.BooApplication
import io.github.toyota32k.ytremote.R
import io.github.toyota32k.ytremote.model.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class PlayerModelBridge(val appViewModel: AppViewModel, val stateModel:PlayerStateModel) {
    companion object {
        val logger = UtLog("EXO")
    }
    lateinit var context:Context
    var player: SimpleExoPlayer? = null
        private set
    var loading:Boolean = false
    var playing:Boolean
        get() = stateModel.isPlaying.value == true
        set(v) { stateModel.isPlaying.value = v }
    var watching = false
    var needToWatch = SuspendableEvent(signal = false, autoReset = false)
    val handler = Handler(Looper.getMainLooper())
    var disabledRanges:List<Range>? = null
    val mediaSourceFactory = ProgressiveMediaSource.Factory(        // ExtractorMediaSource ... non-adaptiveなほとんどのファイルに対応
        DefaultDataSourceFactory(BooApplication.instance, "amv")
    )

    init {
        stateModel.currentItem.observeForever { item->
            if(item!=null) {
                stateModel.onReset()
                player?.apply {
                    setMediaSource(mediaSourceFactory.createMediaSource(MediaItem.fromUri(item.url)), true)
                    prepare()
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
        }
        stateModel.commandNextChapter.bindForever {
            val chapterList = stateModel.chapterSource.chapterInfo.value?.list ?: return@bindForever
            val player = this.player ?: return@bindForever
            val c = chapterList.next(player.currentPosition) ?: return@bindForever
            player.seekTo(c.position)
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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            playAction.isEnabled = !playing
//            pauseAction.isEnabled = playing
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            playAction.setShouldShowIcon(!playing)
//            pauseAction.setShouldShowIcon(playing)
//        }
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

//        override fun onSeekProcessed() {
//            if(!seekManager.isSeeking && !mBindings.isPlaying) {
//                seekCompletedListener.invoke(this@MicVideoPlayer, seekPosition)
//            }
//        }

//        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
//        }

        override fun onPlayerError(error: ExoPlaybackException) {
            logger.stackTrace(error)
            stateModel.onError(context.getString(R.string.error))
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            logger.debug("loading = $isLoading")
            loading = isLoading
            if(isLoading) {
                stateModel.onLoading()
            }
//            player?.also { player->
//                if(isLoading && player.playbackState== Player.STATE_BUFFERING) {
//                    mBindings.playerState = MicVideoPlayer.PlayerState.Loading
//                }
//            }
        }

        override fun onPositionDiscontinuity(reason: Int) {
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        }

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
                    if(dr.size==0) return@withLock

                    val pos = player.currentPosition
                    val hit = dr.filter { it.contains(pos) }.firstOrNull()
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

    /**
     * 絶望的にどんくさいシークを、少し改善するクラス
     *
     * VideoView をやめて、ExoPlayerを使うようにしたことにより、KeyFrame以外へのシークが可能になった。
     * しかし、KeyFrame以外へのシークはかなり遅く、ExoPlayerのステートが、頻繁に、Loading に変化し、
     * シークバーから指を放すまで、プレーヤー画面の表示が更新されない。
     *
     * 実際、デフォルトのコントローラーのスライダーを操作したときも、同じ動作になる。
     *
     * seekモードをCLOSEST_SYNCにると、キーフレームにしかシークしないが、途中の画面も描画されるので、
     * 激しくスライダーを操作しているときは、CLOSEST_SYNCでシークし、止まっているか、ゆっくり操作すると
     * EXACTでシークするようにしてみる。
     */
    inner class SeekManager {
        private val mInterval = 100L        // スライダーの動きを監視するためのタイマーインターバル
        private val mWaitCount = 5          // 上のインターバルで何回チェックし、動きがないことが確認されたらEXACTシークするか？　mInterval*mWaitCount (ms)
        private val mPercent = 1            // 微動（移動していない）とみなす移動量・・・全Durationに対するパーセント
        private var mSeekTarget: Long = -1L // 目標シーク位置
        private var mSeeking = false        // スライダーによるシーク中はtrue / それ以外は false
        private var mCheckCounter = 0       // チェックカウンタ （この値がmWaitCountを超えたら、EXACTシークする）
        private var mThreshold = 0L         // 微動とみなす移動量の閾値・・・naturalDuration * mPercent/100 (ms)
        private var mFastMode = false       // 現在、ExoPlayerに設定しているシークモード（true: CLOSEST_SYNC / false: EXACT）

        // mInterval毎に実行する処理
        private val mLoop = Runnable {
            mCheckCounter++
            checkAndSeek()
        }

        /**
         * Loopの中の人
         */
        private fun checkAndSeek() {
            if(mSeeking) {
                if(mCheckCounter>=mWaitCount && mSeekTarget>=0 ) {
                    if(loading) {
                        UtLogger.debug("EXO-Seek: checked ok, but loading now")
                    } else {
                        UtLogger.debug("EXO-Seek: checked ok")
                        exactSeek(mSeekTarget)
                        mCheckCounter = 0
                    }
                }
                handler.postDelayed(mLoop, mInterval)
            }
        }

        /***
         * スライダーによるシークを開始する
         */
        fun begin(duration:Long) {
            UtLogger.debug("EXO-Seek: begin")
            if(!mSeeking) {
                mSeeking = true
                mFastMode = true
                player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                mSeekTarget = -1L
                mThreshold = (duration * mPercent) / 100
                handler.postDelayed(mLoop, 0)
            }
        }

        /***
         * スライダーによるシークを終了する
         */
        fun end() {
            UtLogger.debug("EXO-Seek: end")
            if(mSeeking) {
                mSeeking = false
                if(mSeekTarget>=0) {
                    exactSeek(mSeekTarget)
                    mSeekTarget = -1
                }
            }
        }

        /***
         * シークを要求する
         */
        fun request(pos:Long) {
            UtLogger.debug("EXO-Seek: request - $pos")
            if(mSeeking) {
                if (mSeekTarget < 0 || (pos - mSeekTarget).absoluteValue > mThreshold) {
                    UtLogger.debug("EXO-Seek: reset check count - $pos ($mCheckCounter)")
                    mCheckCounter = 0
                }
                fastSeek(pos)
                mSeekTarget = pos
            } else {
                exactSeek(pos)
            }
        }

        private fun fastSeek(pos:Long) {
            UtLogger.debug("EXO-Seek: fast seek - $pos")
            if(loading) {
                return
            }
            if(!mFastMode) {
                UtLogger.debug("EXO-Seek: switch to fast seek")
                mFastMode = true
                player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            }
            player?.seekTo(pos)
        }

        private fun exactSeek(pos:Long) {
            UtLogger.debug("EXO-Seek: exact seek - $pos")
            if(mFastMode) {
                UtLogger.debug("EXO-Seek: switch to exact seek")
                mFastMode = false
                player?.setSeekParameters(SeekParameters.EXACT)
            }
            player?.seekTo(pos)
        }

        val isSeeking:Boolean
            get() = mSeeking
    }
    private var seekManager = SeekManager()

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