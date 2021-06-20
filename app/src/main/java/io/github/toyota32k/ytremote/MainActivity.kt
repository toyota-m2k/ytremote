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
import io.github.toyota32k.ytremote.data.NetClient
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
    private lateinit var viewModel: MainViewModel
    private lateinit var drawerAnim : AnimSet
    private lateinit var toolbarAnim : AnimSequence
    private lateinit var binder:MainViewBinder

    inner class MainViewBinder : Binder() {
        val micSpacer:View = findViewById(R.id.mic_spacer)
        val openToolbarButton:View = findViewById(R.id.open_toolbar_button)
        val fab:View  = findViewById(R.id.fab)
        val videoList = findViewById<RecyclerView>(R.id.video_list)
        val showDrawerButton:View = findViewById(R.id.show_drawer_button)
        val drawerGuard:View = findViewById(R.id.drawer_guard)
        val settingButton:View = findViewById(R.id.setting_button)
        val reloadListButton:View = findViewById(R.id.reload_list_button)

        val normalColor = getColor(R.color.list_item_bg)
        val selectedColor = getColor(R.color.purple_700)

        init {
            videoList.layoutManager = LinearLayoutManager(this@MainActivity)
            videoList.setHasFixedSize(true)

            val owner = this@MainActivity
            register(
                viewModel.commandShowDrawer.connectAndBind(owner, showDrawerButton) { viewModel.showSidePanel.value = true },
                viewModel.commandShowDrawer.connectViewEx(openToolbarButton),
                viewModel.commandHideDrawer.connectAndBind(owner, drawerGuard) { viewModel.showSidePanel.value = true },
                viewModel.commandSetting.connectAndBind(owner, settingButton) { startActivity(Intent(this@MainActivity, SettingActivity::class.java)) },
                viewModel.commandPushUrl.connectAndBind(owner, fab) { acceptUrl( it?:return@connectAndBind ) },
                viewModel.commandReloadList.connectAndBind(owner, reloadListButton) { viewModel.refresh() },
                viewModel.commandFullscreen.bind(owner) { showFullscreenViewer(false) },
                viewModel.commandPinP.bind(owner) { showFullscreenViewer(true) },
                RecycleViewBinding.create(owner, videoList, viewModel.videoSources, R.layout.list_item) { binder, view, videoItem ->
//                    val vm = VideoItemViewModel(this@MainActivity, videoItem, viewModel)
                    val textView = view.findViewById<TextView>(R.id.video_item_text)
                    textView.text = videoItem.name
                    binder.register(
                        ClickBinding(owner, textView) { viewModel.appViewModel.currentVideo.value = videoItem },
                        BackgroundBinding.create(this@MainActivity, textView, viewModel.appViewModel.currentVideo.map { ColorDrawable(if(it?.id == videoItem.id) selectedColor else normalColor) })
                    )
                }
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

        private fun showFullscreenViewer(pinp:Boolean) {
            val intent = Intent(this@MainActivity, FullscreenVideoActivity::class.java)
            intent.putExtra(FullscreenVideoActivity.KEY_PINP, pinp)
            startActivity(intent)

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val view = layoutInflater.inflate(R.layout.activity_main, null)
            setContentView(view)
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

        viewModel.videoSources.addListener(this) {
            if( viewModel.playOnMainPlayer.value!=true && (it.kind == ObservableList.MutationKind.REFRESH || it.kind==ObservableList.MutationKind.INSERT)) {
                viewModel.commandShowDrawer.onClick(null)
            }
            if(it.kind==ObservableList.MutationKind.INSERT && viewModel.videoSources.count()>0) {
                binder.videoList.scrollToPosition(viewModel.videoSources.count()-1)
            }
        }

        viewModel.appViewModel.currentVideo.observe(this) {
            val pos = viewModel.videoSources.indexOf(it)
            if(pos>=0) {
                binder.videoList.scrollToPosition(pos)
            }
        }

        viewModel.showSidePanel.distinctUntilChanged().observe(this){
            UtLogger.debug("Drawer:(${it==true})")
            drawerAnim.animate(it==true)
        }

        val med = ToolbarAnimationMediator()
        viewModel.playOnMainPlayer.observe(this) {
            if(it==true) {
                UtLogger.debug("PLY: playing --> hide toolbar")
                //toolbarAnim.animate(false)
                med.request(false, 2000)
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
//                handlers.showDrawer(true)
                UtLogger.debug("PLY: !playing --> show toolbar")
//                toolbarAnim.animate(true)
                med.request(true, 300)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)            }
        }

        if(intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                registerUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
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

    fun registerUrl(rawUrl:String?) {
        if(rawUrl==null||(!rawUrl.startsWith("https://")&&!rawUrl.startsWith("http://"))) {
            return
        }
        val yturl = rawUrl.split("\r","\n"," ","\t").filter {!it.isBlank()}.firstOrNull()
        if(yturl==null) {
            return
        }
        val url = viewModel.appViewModel.settings.urlToRegister(yturl)
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