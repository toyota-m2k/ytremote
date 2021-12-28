@file:Suppress("unused")

package io.github.toyota32k.ytremote.player

import android.annotation.TargetApi
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.exoplayer2.ExoPlayer
import io.github.toyota32k.bindit.Binder
import io.github.toyota32k.utils.UtLogger
import io.github.toyota32k.ytremote.MainActivity
import io.github.toyota32k.ytremote.R
import io.github.toyota32k.ytremote.model.AppViewModel
import io.github.toyota32k.ytremote.model.IPlayerOwner
import java.lang.ref.WeakReference

class FullscreenVideoActivity : AppCompatActivity(), IPlayerOwner {
    /**
     * FullscreenActivity の表示状態
     */
    enum class State {
        NONE,
        FULL,
        PINP
    }

    companion object {
        const val KEY_PINP:String = "pinp"

        private const val INTENT_NAME = "PlayVideo"
        private const val ACTION_TYPE_KEY = "ActionType"

        val supportPinP = MicVideoPlayer.supportPinP

//        // 外部からPinPを閉じるためのメソッド
//        fun close() {
//            activityState.close()
//        }

        /**
         * FullscreenVideoActivity の状態を保持するクラス
         */
        private class ActivityState {
            var state:State = State.NONE
                private set
            private var activity : WeakReference<FullscreenVideoActivity>? = null

            fun onCreated(activity:FullscreenVideoActivity) {
                this.activity = WeakReference(activity)
                this.state = State.FULL
            }
            fun onDestroy() {
                if(activity!=null || state!=State.NONE) {
                    activity = null
                    state = State.NONE
                }
            }
            fun changeState(state:State) {
                this.state = state
            }

            /**
             * PinP表示中の場合にのみ、アクティビティを閉じる
             * (EditorActivity.onDestroy から呼ばれるので、全画面表示のときに閉じるとまずい。
             */
            fun close() {
                if(state==State.PINP) {
                    activity?.get()?.finishAndRemoveTask()
                }
            }
        }
        private val activityState = ActivityState()
    }

    // region Private Fields & Constants

    /**
     * PinP中のアクション
     */
    private enum class Action(val code:Int) {
        PLAY(1),
        PAUSE(2),
        SEEK_TOP(3),
        NEXT(4),
    }

//    private var mSource:Uri? = null
    private var closing:Boolean = false                     // ×ボタンでPinPが閉じられるときに true にセットされる
    private var requestPinP:Boolean = false                 // PinPへの遷移が要求されているか(intentで渡され、ユーザ操作によって変更される）
    private var reloadingPinP:Boolean = false               // onNewIntent()から、PinPへの移行が必要な場合にセットされる
    private var isPinP:Boolean
        get() = appViewModel.playerStateModel.isPinP.value == true
        set(v) { appViewModel.playerStateModel.isPinP.value = v}
    private lateinit var receiver: BroadcastReceiver        // PinP中のコマンド（ブロードキャスト）を受け取るレシーバー
    private lateinit var binder:FSBinder
    private lateinit var appViewModel: AppViewModel

    // endregion
//    private val fsa_player: MicVideoPlayer  by lazy {
//        findViewById(R.id.fullscreen_player)
//    }
//    private val fsa_root: ConstraintLayout by lazy {
//        findViewById(R.id.fullscreen_root)
//    }


    inner class FSBinder: Binder() {
        var playerView: MicVideoPlayer = findViewById(R.id.fullscreen_player)
        var rootContainer: ConstraintLayout = findViewById(R.id.fullscreen_root)

        init {
            val owner = this@FullscreenVideoActivity
            register(
                appViewModel.playerStateModel.updateButtonOnPinP.add(owner) { playing->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        playAction.isEnabled = !playing
                        pauseAction.isEnabled = playing
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        playAction.setShouldShowIcon(!playing)
                        pauseAction.setShouldShowIcon(playing)
                    }
                },
                appViewModel.playerStateModel.commandCloseFullscreen.bind(owner) { startActivity(Intent(owner, MainActivity::class.java)) },
                appViewModel.playerStateModel.commandPinP.bind(owner) {
                    requestPinP = true
                    enterPinP()
                },
            )
        }
    }


    /**
     * 構築
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        UtLogger.debug("##FullscreenVideoActivity.onCreate -- enter")
        super.onCreate(savedInstanceState)

        activityState.onCreated(this)
        setContentView(R.layout.activity_fullscreen_video)
        appViewModel = AppViewModel.instance
        binder = FSBinder()
        appViewModel.attachSecondaryPlayerOwner(this)

        if(null!=intent) {
            initWithIntent(intent)
            if(requestPinP) {
                enterPinP()
            }
        }

        UtLogger.debug("##FullscreenVideoActivity.onCreate -- exit")
    }

    /**
     * 後処理
     */
    override fun onDestroy() {
        super.onDestroy()
        activityState.onDestroy()
//        mSource = null
        binder.dispose()
        appViewModel.detachPlayerOwner(this)
        UtLogger.debug("##FullscreenVideoActivity.onDestroy")
    }

    /**
     * このActivityは singleTask モードで実行されるので、すでにActivityが存在する状態で、startActivity されると、
     * onCreate()ではなく、onNewIntent()が呼ばれる。
     * 渡されたインテントに基づいて、適切に状態を再構成する
     */
    override fun onNewIntent(intent: Intent?) {
        UtLogger.debug("##FullscreenVideoActivity.onNewIntent")
        super.onNewIntent(intent)
        if(intent!=null) {
            this.intent = intent
            initWithIntent(intent)
            if(requestPinP && isPinP) {
                reloadingPinP = true
            }
        }
    }

    /**
     * インテントで渡された情報をもとにビューを初期化する
     * onCreate / onNewIntent 共通の処理
     */
    private fun initWithIntent(intent:Intent) {
//        val source = intent.getStringExtra(KEY_SOURCE)
        requestPinP = intent.getBooleanExtra(KEY_PINP, false)
//        if (null != source) {
//            val playing = intent.getBooleanExtra(KEY_PLAYING, false)
//            val position = intent.getLongExtra(KEY_POSITION, 0)
//            val start = intent.getLongExtra(KEY_CLIP_START, -1)
//            val end = intent.getLongExtra(KEY_CLIP_END, -1)
//            if (start >= 0) {
//                fsa_player.setClip(MicClipping(start, end))
//            }
//            mSource = Uri.parse(source)
//            fsa_player.setSource(mSource!!, playing, position)
//        }
    }

    /**
     * PinPに遷移する
     * （PinPから通常モードへの遷移はシステムに任せる。というより、そのようなAPIは存在しない。）
     */
    @TargetApi(Build.VERSION_CODES.O)
    private fun enterPinP() {
        if (supportPinP) {
            val param = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16,9))
                    .setActions(listOf(playAction, pauseAction, nextMovieAction))
                    .build()
            enterPictureInPictureMode(param)
        }
    }

    /**
     * PinP内のPlayボタン
     */
    private val playAction: RemoteAction by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = this@FullscreenVideoActivity
            val icon = Icon.createWithResource(context, R.drawable.ic_play)
            val title = context.getText(R.string.play)
            val pendingIntent = PendingIntent.getBroadcast(context, Action.PLAY.code, Intent(INTENT_NAME).putExtra(ACTION_TYPE_KEY, Action.PLAY.code),0)
            RemoteAction(icon, title, title, pendingIntent)
        } else {
            throw IllegalStateException("needs Android O or later.")
        }
    }

    /**
     * PinP内のPauseボタン
     */
    private val pauseAction:RemoteAction by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = this@FullscreenVideoActivity
            val icon = Icon.createWithResource(context, R.drawable.ic_pause)
            val title = context.getText(R.string.pause)
            val pendingIntent = PendingIntent.getBroadcast(context, Action.PAUSE.code, Intent(INTENT_NAME).putExtra(ACTION_TYPE_KEY, Action.PAUSE.code),0)
            RemoteAction(icon, title, title, pendingIntent)
        } else {
            throw IllegalStateException("needs Android O or later.")
        }
    }

    /**
     * 先頭へシーク
     */
    private val seekTopAction:RemoteAction by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = this@FullscreenVideoActivity
            val icon = Icon.createWithResource(context, R.drawable.ic_back)
            val title = context.getText(R.string.seekTop)
            val pendingIntent = PendingIntent.getBroadcast(context, Action.SEEK_TOP.code, Intent(INTENT_NAME).putExtra(ACTION_TYPE_KEY, Action.SEEK_TOP.code),0)
            RemoteAction(icon, title, title, pendingIntent)
        } else {
            throw IllegalStateException("needs Android O or later.")
        }
    }

    /**
     * 先頭へシーク
     */
    private val nextMovieAction:RemoteAction by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = this@FullscreenVideoActivity
            val icon = Icon.createWithResource(context, R.drawable.ic_next)
            val title = context.getText(R.string.next_button)
            val pendingIntent = PendingIntent.getBroadcast(context, Action.NEXT.code, Intent(INTENT_NAME).putExtra(ACTION_TYPE_KEY, Action.NEXT.code),0)
            RemoteAction(icon, title, title, pendingIntent)
        } else {
            throw IllegalStateException("needs Android O or later.")
        }
    }

    override fun onRestart() {
        UtLogger.debug("##FullscreenVideoActivity.onRestart")
        super.onRestart()
    }

    override fun onStart() {
        UtLogger.debug("##FullscreenVideoActivity.onStart")
        super.onStart()
    }

    override fun onResume() {
        UtLogger.debug("##FullscreenVideoActivity.onResume")
        super.onResume()
        // もともと、次のフラグは、onCreate()で設定していたが、
        // Full<->PinPを行き来していると、いつの間にか、一部フラグが落ちて NavBarが表示されてしまうので、resume時にセットすることにした。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            binder.rootContainer.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    override fun onPause() {
        UtLogger.debug("##FullscreenVideoActivity.onPause")
        super.onPause()
    }

    /**
     * PinP画面で、×ボタンを押したとき（閉じる）と、□ボタンを押したとき（全画面に戻る）で、
     * onPictureInPictureModeChanged()のパラメータに区別がないのだが、
     * ×を押したときは、onPictureInPictureModeChangedが呼ばれる前に onStop()が呼ばれ、□ボタンの場合は呼ばれないことが分かったので、
     * これによって、×と□を区別する。
     *
     * Android8/9 では上記の通りだったが、Android10 では、onStop より先に、onPictureInPictureModeChanged が呼ばれるようになった。
     * つまり、onStop()が呼ばれるタイミングで、Android8/9 なら、PinP中の場合と、FullScreenの場合が混在するのに対して、
     * android10 では、必ず（PinPは解除されて）FullScreenの状態になっている。このことから、PinPモードでなければ(FullScreenなら）、
     * onStop()でアクティビティを終了すれば、PinP解除＋終了というフローが維持できる。副作用として、最初からFullScreenとして起動している場合に、
     * タスクマネージャなどを表示して、アクティビティを切り替えようとしただけで、このアクティビティが終了してしまうが、
     * それ自体が、望ましい動作（FullscreenActivityを残したまま、MainActivityに戻るのを禁止したい）なので、たぶん問題はないと思われる。
     */
    override fun onStop() {
        UtLogger.debug("##FullscreenVideoActivity.onStop")
        closing = true
//        intent.putExtra(KEY_PLAYING, fsa_player.isPlayingOrReservedToPlay)
//        intent.putExtra(KEY_POSITION, fsa_player.seekPosition)
//        fsa_player.pause()
        super.onStop()
//        onResultListener.invoke(intent)
        if(!isPinP) {
            // ここに入ってくるのは、
            // - 全画面表示中に、閉じるボタンがタップされた場合(Android 9,10共通)
            // - PinP中に、×ボタンがタップされたとき(Android 10のみ）
            //
            // PinPの×ボタンを押すと、
            //  Android 10 の場合は、onPictureInPictureModeChanged で、isPinP=false にされたあと、onStopが呼ばれる。
            //  Android 9- の場合は、onStop()が呼ばれてから onPictureInPictureModeChanged が呼ばれるので、isPinPはまだtrue。
            finishAndRemoveTask()
        }
    }

//    override fun onPostResume() {
//        UtLogger.debug("##FullscreenVideoActivity.onPostResume")
//        super.onPostResume()
//    }

    /**
     * PinPモードが開始される
     */
    private fun onEnterPinP() {
        activityState.changeState(State.PINP)
        binder.playerView.showDefaultController = false
        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null||intent.action!=INTENT_NAME) {
                    return
                }

                when(intent.getIntExtra(ACTION_TYPE_KEY, -1)) {
                    Action.PAUSE.code -> binder.playerView.pause()
                    Action.PLAY.code -> binder.playerView.play()
                    Action.SEEK_TOP.code -> binder.playerView.seekStart()
                    Action.NEXT.code -> appViewModel.nextVideo()
                    else -> {}
                }
            }
        }
        registerReceiver(receiver, IntentFilter(INTENT_NAME))
    }

    /**
     * PinPモードが終了する
     */
    private fun onExitPinP() {
        activityState.changeState(State.FULL)
        unregisterReceiver(receiver)
        if(closing) {
            // Android 9- の場合に、
            // ×ボタンがタップされた(＝ここに来る前にonStopが呼ばれている）
            // --> Activityを閉じる
            finishAndRemoveTask()
        } else {
            binder.playerView.showDefaultController = true
            if(reloadingPinP) {
                // PinP中に、onNewIntent()で、pinpでの実行が要求された
                // onNewIntent()が呼ばれるケースでは、システムが、PinPモードを強制的に解除してくるので、ここに入ってくる。
                // このときは（他にうまい方法があればよいのだが）少し時間をあけて、PinPへの移行を要求する。
                // ちなみに、手持ちの Huawei の場合、100ms ではダメで、500ms ならokだった。
                reloadingPinP = false
                Handler(Looper.getMainLooper()).postDelayed({
                    enterPinP()
                }, 500)
            } else {
                // □ボタンがおされてPinPからFullScreenに移行しようとしている
                requestPinP = false
            }
        }
    }

    /**
     * PinPモードが変更されるときの通知
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        UtLogger.debug("##FullscreenVideoActivity.onPictureInPictureModeChanged($isInPictureInPictureMode)")
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPinP = isInPictureInPictureMode
        if(!isInPictureInPictureMode) {
            onExitPinP()
        } else {
            onEnterPinP()
        }
    }

    override fun ownerResigned() {
        binder.playerView.unbindPlayer()
    }

    override fun ownerAssigned(player: ExoPlayer) {
        binder.playerView.bindPlayer(player,enableFullscreen = false,enablePinP = true,enableClose = true)
    }
}