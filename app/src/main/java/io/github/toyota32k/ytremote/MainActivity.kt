package io.github.toyota32k.ytremote

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.toyota32k.bindit.BackgroundBinding
import io.github.toyota32k.bindit.Binder
import io.github.toyota32k.bindit.ClickBinding
import io.github.toyota32k.bindit.RecycleViewBinding
import io.github.toyota32k.bindit.list.ObservableList
import io.github.toyota32k.utils.UtLogger
import io.github.toyota32k.utils.disposableObserve
import io.github.toyota32k.ytremote.data.CurrentItemSynchronizer
import io.github.toyota32k.ytremote.data.NetClient
import io.github.toyota32k.ytremote.fragment.HomeFragment
import io.github.toyota32k.ytremote.model.MainViewModel
import io.github.toyota32k.ytremote.player.FullscreenVideoActivity
import io.github.toyota32k.ytremote.utils.AnimSequence
import io.github.toyota32k.ytremote.utils.AnimSet
import io.github.toyota32k.ytremote.utils.ViewSizeAnimChip
import io.github.toyota32k.ytremote.utils.ViewVisibilityAnimationChip
import kotlinx.coroutines.*
import okhttp3.Request
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        val logger get() = MainViewModel.logger
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var drawerAnim : AnimSet
    private lateinit var toolbarAnim : AnimSequence
    private lateinit var binder:MainViewBinder
    private val appViewModel get() = viewModel.appViewModel

    inner class MainViewBinder : Binder() {
        val micSpacer:View = findViewById(R.id.mic_spacer)
        val openToolbarButton:View = findViewById(R.id.open_toolbar_button)
        val fab:View  = findViewById(R.id.fab)
        val drawerGuard:View = findViewById(R.id.drawer_guard)

        private val videoList: RecyclerView = findViewById(R.id.video_list)
        private val showDrawerButton:View = findViewById(R.id.show_drawer_button)
        private val settingButton:View = findViewById(R.id.setting_button)
        private val reloadListButton:View = findViewById(R.id.reload_list_button)
        private val syncToHostButton:View = findViewById(R.id.sync_to_host)
        private val syncFromHostButton:View = findViewById(R.id.sync_from_host)

        private val normalColor = getColor(R.color.list_item_bg)
        private val selectedColor = getColor(R.color.purple_700)

        init {
            val owner = this@MainActivity
            videoList.layoutManager = LinearLayoutManager(owner)
            videoList.setHasFixedSize(true)

            val toolbarAnimationMediator = ToolbarAnimationMediator()
            register(
                viewModel.commandShowDrawer.connectAndBind(owner, showDrawerButton) { viewModel.showSidePanel.value = true },
                viewModel.commandShowDrawer.connectViewEx(openToolbarButton),
                viewModel.commandHideDrawer.connectAndBind(owner, drawerGuard) { viewModel.showSidePanel.value = false },
                viewModel.commandSetting.connectAndBind(owner, settingButton) { startActivity(Intent(owner, SettingActivity::class.java)) },
                viewModel.commandPushUrl.connectAndBind(owner, fab) { acceptUrl( it?:return@connectAndBind ) },
                viewModel.commandReloadList.connectAndBind(owner, reloadListButton) { appViewModel.refreshVideoList() },
                viewModel.commandSyncFromHost.connectAndBind(owner, syncFromHostButton) { CurrentItemSynchronizer.syncFrom() },
                viewModel.commandSyncToHost.connectAndBind(owner, syncToHostButton) { CurrentItemSynchronizer.syncTo() },
                appViewModel.playerStateModel.commandFullscreen.bind(owner) { showFullscreenViewer(false) },
                appViewModel.playerStateModel.commandPinP.bind(owner) { showFullscreenViewer(true) },
                RecycleViewBinding.create(owner, videoList, appViewModel.videoSources, R.layout.list_item) { binder, view, videoItem ->
//                    val vm = VideoItemViewModel(owner, videoItem, viewModel)
                    val textView = view.findViewById<TextView>(R.id.video_item_text)
                    textView.text = videoItem.name
                    binder.register(
                        ClickBinding(owner, textView) { viewModel.appViewModel.currentItem.value = videoItem },
                        BackgroundBinding.create(owner, textView, viewModel.appViewModel.currentItem.map { ColorDrawable(if(it?.id == videoItem.id) selectedColor else normalColor) })
                    )
                },
                appViewModel.videoSources.addListener(owner ) {
                    // MainActivity上で再生中に、プレイリストが更新されたら、サイドバーを表示し、挿入位置までスクロールする
                    if( viewModel.player.value!=null && (it.kind == ObservableList.MutationKind.REFRESH || it.kind==ObservableList.MutationKind.INSERT)) {
                        viewModel.showSidePanel.value = true
                        if(it.kind==ObservableList.MutationKind.INSERT && appViewModel.videoSources.count()>0) {
                            binder.videoList.scrollToPosition(kotlin.math.min((it as ObservableList.InsertEventData).position, appViewModel.videoSources.count()-1))
                        }
                    }
                },
                viewModel.appViewModel.currentItem.disposableObserve(owner) {
                    // 再生ターゲットが変わったときに、それをリスト内に表示するようスクロール
                    val pos = appViewModel.videoSources.indexOf(it)
                    if(pos>=0) {
                        binder.videoList.scrollToPosition(pos)
                    }
                },
                viewModel.showSidePanel.distinctUntilChanged().disposableObserve(owner){
                    // サイドバーの表示・非表示切り替え動作
                    UtLogger.debug("Drawer:(${it==true})")
                    drawerAnim.animate(it==true)
                },
                viewModel.isPlayingOnMainView.disposableObserve(owner) {
                    // 再生中はツールバーを隠す
                    if (it == true) {
                        UtLogger.debug("PLY: playing --> hide toolbar")
                        toolbarAnimationMediator.request(false, 2000)
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        UtLogger.debug("PLY: !playing --> show toolbar")
                        toolbarAnimationMediator.request(true, 300)
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                },

            )
        }

        // BooサーバーにURLをリクエストする
        private fun acceptUrl(anchor:View) {
            val url = urlFromClipboard()
            if(!url.isNullOrBlank()) {
                registerUrl(url)
                Snackbar.make(anchor, url, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            } else {
                Snackbar.make(anchor, "No data to request.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }

        // FullScreen/PinP表示に切り替える
        private fun showFullscreenViewer(pinp:Boolean) {
            val intent = Intent(this@MainActivity, FullscreenVideoActivity::class.java)
            intent.putExtra(FullscreenVideoActivity.KEY_PINP, pinp)
            startActivity(intent)

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        logger.debug()
        super.onCreate(savedInstanceState)
        try {
            setContentView(View.inflate(this, R.layout.activity_main, null))
        } catch (e:Throwable) {
            UtLogger.stackTrace(e)
        }
        setContentView(R.layout.activity_main)

        viewModel = MainViewModel.instanceFor(this)
        binder = MainViewBinder()

        drawerAnim = AnimSet().apply {
            add(ViewSizeAnimChip(binder.micSpacer, 240, 0, height=false))
            add(ViewVisibilityAnimationChip(binder.drawerGuard, startVisible = true, endVisible = false, gone = true, maxAlpha = 0.6f))
        }
        toolbarAnim = AnimSequence().apply {
            add( AnimSet().apply {
                add(ViewSizeAnimChip(binder.micSpacer, 40, 0, height = true))
            })
            add(AnimSet().apply{
                add(ViewVisibilityAnimationChip(binder.openToolbarButton, startVisible = false, endVisible = true))
                add(ViewVisibilityAnimationChip(binder.fab, startVisible = true, endVisible = false))
            })
        }

        if(intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                registerUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
        }

        if(savedInstanceState==null) {
            val homeFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment)
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if(intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                registerUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
        }
    }

    override fun onDestroy() {
        logger.debug()
        super.onDestroy()
        binder.dispose()
    }

    fun registerUrl(rawUrl:String?) {
        if(rawUrl==null||(!rawUrl.startsWith("https://")&&!rawUrl.startsWith("http://"))) {
            return
        }
        val urlParam = rawUrl.split("\r", "\n", " ", "\t").firstOrNull { it.isNotBlank() } ?: return
        val url = viewModel.appViewModel.settings.urlToRegister(urlParam)
        CoroutineScope(Dispatchers.Default).launch {
            val req = Request.Builder()
                    .url(url)
                    .get()
                    .build()
            NetClient.executeAsync(req)
        }
    }

    fun urlFromClipboard():String? {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        if(cm.hasPrimaryClip()) {
            val item = cm.primaryClip?.getItemAt(0) ?: return null
            val txt = item.text as? String
            if (txt != null && txt.startsWith("https://")) {
                return txt
            }
            val url = item.uri
            if (url != null) {
                return url.toString()
            }
        }
        return null
    }

    /**
     * 動画再生開始、停止時のツールバー表示・非表示アニメーションが連続で実行されてがちょんがちょんなるのを防ぐための調停者
     */
    inner class ToolbarAnimationMediator {
        private var requested = false
        private var actual = true
        private var deferred: Deferred<Unit>? = null

        fun request(show:Boolean, after:Long) {
            deferred?.cancel("cancel")
            requested = show
            deferred = CoroutineScope(Dispatchers.Main).async {
                delay(after)
                if(requested!=actual) {
                    actual = requested
                    toolbarAnim.animate(requested)
                    viewModel.showSidePanel.value = requested
                }
            }
        }
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if(!viewModel.appViewModel.settings.isValid) {
            viewModel.commandSetting.onClick(null)
        }
    }

}