package com.michael.ytremote

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.Space
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.michael.ytremote.data.VideoItem
import com.michael.ytremote.databinding.ActivityMainBinding
import com.michael.ytremote.databinding.ListItemBinding
import com.michael.ytremote.model.VideoItemViewModel
import com.michael.ytremote.model.MainViewModel
import com.michael.ytremote.utils.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var handlers:Handlers
    private lateinit var drawerAnim :AnimPack
    private lateinit var toolbarAnim :AnimPack

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = MainViewModel.instanceFor(this)
        handlers = Handlers()
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
            lifecycleOwner = this@MainActivity
            model = viewModel
            handler = this@MainActivity.handlers
        }
        drawerAnim = AnimPack().apply {
            addAnim(binding.micSpacer, 240f, 0f) {o,v->
                (o as View).setLayoutWidth(dp2px(v.toInt()))
            }
            addAnim(binding.micDrawerGuard, 0.5f, 0f) {o,v->
                (o as View).alpha = v
            }
            onStart = { show->
                if(show) {
                    binding.micDrawerGuard.visibility = View.VISIBLE
                }
            }
            onCompleted = { show ->
                if(!show) {
                    binding.micDrawerGuard.visibility = View.GONE
                }
            }
        }
        toolbarAnim = AnimPack().apply {
            addAnim(binding.micSpacer, 40f, 0f) {o,v->
                (o as View).setLayoutHeight(dp2px(v.toInt()))
            }
            onCompleted = { show ->
                val va = VisibilityAnimPack().apply {
                    addView(binding.fab)
                    addView(binding.micOpenToolbar,true)
                    animate(show)
                }
            }
        }
        binding.micOpenToolbar.visibility = View.VISIBLE

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        viewModel.appViewModel.playing.observe(this) {
        }

        binding.videoList.run {
            adapter = ListAdapter()
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }
        viewModel.videoList.observe(this) {
            (binding.videoList.adapter as? ListAdapter)?.items = it
        }
        viewModel.resetSidePanel.observe(this){
            handlers.showDrawer(false)
        }
        viewModel.playOnMainPlayer.observe(this) {
            toolbarAnim.animate(!(it==true))
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if(savedInstanceState==null) {
            viewModel.update()
        }
    }

    inner class Handlers {
        fun showDrawer(show:Boolean) {
            drawerAnim.animate(show)
        }
        fun showToolbar(show:Boolean) {
            binding.micSpacer.setLayoutHeight(if(show) dp2px(40) else 0)
        }
    }

    inner class ViewHolder(private val binding:ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(videoItem:VideoItem) {
            binding.model = VideoItemViewModel(videoItem, viewModel)
            binding.executePendingBindings()
        }
    }

    inner class ListAdapter() : RecyclerView.Adapter<ViewHolder>() {
        var items:List<VideoItem>? = null
            set(v) {
                field = v
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DataBindingUtil.inflate<ListItemBinding>(LayoutInflater.from(parent.context), R.layout.list_item, parent, false).apply {
                lifecycleOwner = parent.lifecycleOwner()
            }
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val v = items?.get(position) ?: return
            holder.bind(v)
        }

        override fun getItemCount(): Int {
            return items?.count() ?: 0
        }

    }
}