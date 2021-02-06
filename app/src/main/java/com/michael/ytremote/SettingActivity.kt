package com.michael.ytremote

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.michael.ytremote.databinding.ActivitySettingBinding
import com.michael.ytremote.databinding.HostListItemBinding
import com.michael.ytremote.model.HostItemViewModel
import com.michael.ytremote.model.SettingViewModel
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

        // アドレス入力欄でのENTERキーの処理
        binding.hostAddrEdit.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                viewModel.addHost()
                true
            } else false
        }

        // ホストリストの初期化
        binding.hostList.run {
            adapter = ListAdapter()
            layoutManager = LinearLayoutManager(this@SettingActivity)
            setHasFixedSize(true)
        }
        viewModel.hostList.observe(this) {
            (binding.hostList.adapter as? ListAdapter)?.items = it
        }

        // ラジオボタン、トグルボタン (SourceType, Rating, Mark)
        viewModel.sourceTypeGroup.bind(binding.sourcrTypeSelector, true)
        viewModel.ratingGroup.bind(binding.ratingSelector, true)
        viewModel.markGroup.bind(binding.markSelector, true)

        // カテゴリー（ポップアップメニュー）
        binding.categoryButton.setOnClickListener(this::selectCategory)
        viewModel.categoryList.currentLabel.observe(this) {
            binding.categoryButton.text = it ?: "All"
        }

        // アクションバー
        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private var listPopup: ListPopupWindow? = null
    private fun selectCategory(view: View) {
        val adapter = ArrayAdapter<String>(
                this,
                R.layout.array_adapter_item,
                viewModel.categoryList.list.value?.map {it.label}?.toTypedArray() ?: arrayOf("All"))
        listPopup = ListPopupWindow(this, null, R.attr.listPopupWindowStyle)
                .apply {
                    setAdapter(adapter)
                    anchorView = view
                    setOnItemClickListener { parent, view, position, id ->
                        val cat = adapter.getItem(position)
                        if(null!=cat) {
                            viewModel.categoryList.category = cat
                        }
                        listPopup?.dismiss()
                        listPopup = null
                    }
                    show()
                }
    }

    override fun onBackPressed() {
        checkAndFinish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                checkAndFinish(); true
            }
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

    inner class ListAdapter : RecyclerView.Adapter<ViewHolder>() {
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