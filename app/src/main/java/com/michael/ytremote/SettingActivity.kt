package com.michael.ytremote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michael.ytremote.data.Rating
import com.michael.ytremote.data.VideoItem
import com.michael.ytremote.databinding.ActivityMainBinding
import com.michael.ytremote.databinding.ActivitySettingBinding
import com.michael.ytremote.databinding.HostListItemBinding
import com.michael.ytremote.databinding.ListItemBinding
import com.michael.ytremote.model.HostItemViewModel
import com.michael.ytremote.model.SettingViewModel
import com.michael.ytremote.model.VideoItemViewModel
import com.michael.ytremote.utils.UtLogger
import com.michael.ytremote.utils.lifecycleOwner

class SettingActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingBinding
    lateinit var viewModel: SettingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SettingViewModel.instanceFor(this)
        if(savedInstanceState==null) {
            viewModel.load(this)
        }
        binding = DataBindingUtil.setContentView<ActivitySettingBinding>(this, R.layout.activity_setting).apply {
            model = viewModel
        }
//        setContentView(R.layout.activity_setting)

        binding.hostList.run {
            adapter = ListAdapter()
            layoutManager = LinearLayoutManager(this@SettingActivity)
            setHasFixedSize(true)
        }
        viewModel.hostList.observe(this) {
            (binding.hostList.adapter as? ListAdapter)?.items = it
        }


        viewModel.ratingGroup.bind(binding.ratingSelector, true)

        viewModel.markGroup.bind(binding.markSelector, true)

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home-> { checkAndFinish(); true }
            else-> super.onOptionsItemSelected(item)
        }
    }

    private fun checkAndFinish() {
        UtLogger.debug("Rating=${viewModel.ratingGroup.rating}")
        UtLogger.debug("Marks=${viewModel.markGroup.marks}")

        if(!viewModel.save(this)) {
            return
        }
        finish()
    }


    inner class ViewHolder(private val binding: HostListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(address: String) {
            binding.model = HostItemViewModel(address, viewModel)
            binding.executePendingBindings()
        }
    }

    inner class ListAdapter() : RecyclerView.Adapter<ViewHolder>() {
        var items:List<String>? = null
            set(v) {
                field = v
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DataBindingUtil.inflate<HostListItemBinding>(LayoutInflater.from(parent.context), R.layout.host_list_item, parent, false).apply {
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