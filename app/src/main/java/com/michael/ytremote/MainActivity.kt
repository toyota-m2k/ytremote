package com.michael.ytremote

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.distinctUntilChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.michael.bindit.Binder
import com.michael.bindit.impl.RecycleViewBinding
import com.michael.ytremote.bind.list.ObservableList
import com.michael.ytremote.bind.list.RecyclerViewAdapter
import com.michael.ytremote.data.NetClient
import com.michael.ytremote.data.VideoItem
import com.michael.ytremote.databinding.ActivityMainBinding
import com.michael.ytremote.databinding.ListItemBinding
import com.michael.ytremote.model.MainViewModel
import com.michael.ytremote.model.VideoItemViewModel
import com.michael.ytremote.utils.*
import kotlinx.coroutines.*
import okhttp3.Request
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var handlers:Handlers
    private lateinit var drawerAnim :AnimSet
    private lateinit var toolbarAnim :AnimSequence
    private lateinit var binder:Binder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = Binder()
        viewModel = MainViewModel.instanceFor(this)
        handlers = Handlers()
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            lifecycleOwner = this@MainActivity
            model = viewModel
            handler = this@MainActivity.handlers
        }
        drawerAnim = AnimSet().apply {
            add(ViewSizeAnimChip(binding.micSpacer, 240, 0, height=false))
            add(ViewVisibilityAnimationChip(binding.micDrawerGuard, startVisible = true, endVisible = false, gone = true, maxAlpha = 0.6f))
        }
        toolbarAnim = AnimSequence().apply {
            add( AnimSet().apply {
                add(ViewSizeAnimChip(binding.micSpacer, 40, 0, height = true))
            })
            add(AnimSet().apply{
                add(ViewVisibilityAnimationChip(binding.micOpenToolbar, startVisible = false, endVisible = true))
            })
        }

        binding.videoList.run {
//            adapter = ListAdapter()
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }

        viewModel.videoSources.addListener(this) {
            if( viewModel.playOnMainPlayer.value!=true && (it.kind == ObservableList.MutationKind.REFRESH || it.kind==ObservableList.MutationKind.INSERT)) {
                handlers.showDrawer(true)
            }
        }

        binder.register(RecycleViewBinding(
            viewModel.videoSources, binding.videoList).apply {
                view.adapter = RecyclerViewAdapter.SimpleWithDataBinding<VideoItem, ListItemBinding>(this@MainActivity, list,
                    createView = { parent, _ ->
                        DataBindingUtil.inflate<ListItemBinding>(LayoutInflater.from(parent.context), R.layout.list_item, parent, false).apply { lifecycleOwner = parent.lifecycleOwner() }
                    },
                    bind = { b, item ->
                        b.model = VideoItemViewModel(item, viewModel)
                        b.executePendingBindings()
                    })
            }
        )

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
                handlers.showDrawer(true)
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
                }
            }
        }
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if(!viewModel.appViewModel.settings.isValid) {
            handlers.openSetting()
        }
    }

//    override fun onResume() {
//        super.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//    }

    inner class Handlers {
        fun showDrawer(show:Boolean) {
            viewModel.showSidePanel.value = show
        }
//        fun showToolbar(show:Boolean) {
//            toolbarAnim.animate(show)
//        }

        fun refresh() {
            viewModel.refresh()
        }


        fun openSetting() {
            startActivity(Intent(this@MainActivity, SettingActivity::class.java))
        }
    }

//    inner class ViewHolder(private val binding:ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun bind(videoItem:VideoItem) {
//            binding.model = VideoItemViewModel(videoItem, viewModel)
//            binding.executePendingBindings()
//        }
//    }
//
//    inner class ListAdapter : RecyclerView.Adapter<ViewHolder>() {
//        var items:List<VideoItem>? = null
//            set(v) {
//                field = v
//                notifyDataSetChanged()
//            }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//            val binding = DataBindingUtil.inflate<ListItemBinding>(LayoutInflater.from(parent.context), R.layout.list_item, parent, false).apply {
//                lifecycleOwner = parent.lifecycleOwner()
//            }
//            return ViewHolder(binding)
//        }
//
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            val v = items?.get(position) ?: return
//            holder.bind(v)
//        }
//
//        override fun getItemCount(): Int {
//            return items?.count() ?: 0
//        }
//
//    }
}