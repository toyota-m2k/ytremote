package io.github.toyota32k.ytremote

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.toyota32k.bindit.*
import io.github.toyota32k.utils.UtLogger
import io.github.toyota32k.ytremote.data.Mark
import io.github.toyota32k.ytremote.data.Rating
import io.github.toyota32k.ytremote.data.SourceType
import io.github.toyota32k.ytremote.model.SettingViewModel
import io.github.toyota32k.ytremote.utils.PackageUtil

class SettingActivity : AppCompatActivity() {
    lateinit var viewModel: SettingViewModel
    lateinit var binder:SettingBinder

    class SettingBinder(activity:SettingActivity, model:SettingViewModel) : Binder() {
        val sourceTypeSelector = activity.findViewById<RadioGroup>(R.id.sourcr_type_selector)
        val ratingSelector = activity.findViewById<MaterialButtonToggleGroup>(R.id.rating_selector)
        val markSelector = activity.findViewById<MaterialButtonToggleGroup>(R.id.mark_selector)
        val settingsToolbar = activity.findViewById<Toolbar>(R.id.settingsToolbar)
        val hostAddrEdit = activity.findViewById<EditText>(R.id.host_addr_edit)
        val addToListButton = activity.findViewById<View>(R.id.add_to_list_button)
        val hostList = activity.findViewById<RecyclerView>(R.id.host_list)
        val categoryButton = activity.findViewById<Button>(R.id.category_button)

        init {
            settingsToolbar.title ="${PackageUtil.appName(activity)} - v${PackageUtil.getVersion(activity)}"
            hostList.layoutManager = LinearLayoutManager(activity)
            hostList.setHasFixedSize(true)

            register(
                RadioGroupBinding.create(activity, sourceTypeSelector, model.sourceType, SourceType.idResolver, BindingMode.TwoWay),
                MaterialRadioButtonGroupBinding.create(activity, ratingSelector, model.rating, Rating.idResolver, BindingMode.TwoWay),
                MaterialToggleButtonGroupBinding.create(activity, markSelector, model.markList, Mark.idResolver, BindingMode.TwoWay),
                EditTextBinding.create(activity, hostAddrEdit, model.editingHost),
                TextBinding.create(activity, categoryButton, model.categoryList.currentLabel.map { it ?: "All" }),
                model.commandAddToList.connectAndBind(activity, addToListButton) { model.addHost() },
                model.commandAddToList.connectViewEx(hostAddrEdit),
                model.commandCategory.connectAndBind(activity, categoryButton, activity::selectCategory),

                RecycleViewBinding.createHeightWrapContent<String>(activity, hostList, model.hostList.value!!, R.layout.host_list_item) { binder, view, address ->
                    val textView = view.findViewById<TextView>(R.id.address_text)
                    textView.text = address
                    binder.register(
                        ClickBinding(activity, view.findViewById(R.id.item_container)) {  model.activeHost.value = address },
                        ClickBinding(activity, view.findViewById(R.id.del_button)) {  model.removeHost(address) },
                        VisibilityBinding.create(activity, view.findViewById(R.id.check_mark), model.activeHost.map { it==address }, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByInvisible),
                    )
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        viewModel = SettingViewModel.instanceFor(this)
        if(savedInstanceState==null) {
            viewModel.load(this)
        }
        binder = SettingBinder(this,viewModel)

        // アクションバー
        setSupportActionBar(binder.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private var listPopup: ListPopupWindow? = null
    private fun selectCategory(view: View?) {
        val adapter = ArrayAdapter<String>(
                this,
                R.layout.array_adapter_item,
                viewModel.categoryList.list.value?.map {it.label}?.toTypedArray() ?: arrayOf("All"))
        listPopup = ListPopupWindow(this, null, R.attr.listPopupWindowStyle)
                .apply {
                    setAdapter(adapter)
                    anchorView = view
                    setOnItemClickListener { _, _, position, _ ->
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
        UtLogger.debug("Rating=${viewModel.rating.value}")
        UtLogger.debug("Marks=${viewModel.markList.value}")

        if(!viewModel.save(this)) {
            return
        }
        finish()
    }
}