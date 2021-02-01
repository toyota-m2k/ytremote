package com.michael.ytremote.model

import android.content.Context
import androidx.lifecycle.*
import com.google.android.material.button.MaterialButtonToggleGroup
import com.michael.ytremote.data.Mark
import com.michael.ytremote.data.Rating
import com.michael.ytremote.data.Settings
import com.michael.ytremote.data.SourceType
import com.michael.ytremote.utils.RadioButtonGroup
import com.michael.ytremote.utils.ToggleButtonGroup
import com.michael.ytremote.utils.utAssert
import java.lang.ref.WeakReference

class RatingRadioGroup : RadioButtonGroup<Rating>() {
    override fun id2value(id: Int): Rating? {
        return Rating.values().find {it.id==id}
    }
    override fun value2id(v: Rating): Int {
        return v.id
    }

    var rating:Rating?
        get() = selected
        set(v) { selected = v }
}

class MarkToggleGroup : ToggleButtonGroup<Mark>() {
    override fun id2value(id: Int): Mark? {
        return Mark.values().find { it.id == id }
    }

    override fun value2id(v: Mark): Int {
        return v.id
    }

    var marks:List<Mark>
        get() = selected
        set(v) { selected = v }
}

class SettingViewModel : ViewModel() {
    val activeHost = MutableLiveData<String>()
    val editingHost = MutableLiveData<String>()
    val hostList = MutableLiveData<List<String>>()
    val sourceType = MutableLiveData<SourceType>()

    val ratingGroup = RatingRadioGroup()
    val markGroup = MarkToggleGroup()

    val category=MutableLiveData<String>()

    val srcTypeButton = sourceType.map { it.id }

    fun hasHost(address:String) : Boolean {
        return null != hostList.value?.find {it==address}
    }

    fun addHost() {
        addHost(address = editingHost.value ?: return)
    }

    fun addHost(address:String) {
        if(address.isNotBlank() && !hasHost(address)) {
            hostList.value = (hostList.value?.toMutableList() ?: mutableListOf()).apply { add(address) }
            activeHost.value = address
        }
    }

    fun removeHost(address:String) {
        hostList.value = (hostList.value?.toMutableList() ?: mutableListOf()).filter { it!=address }
        if(activeHost.value == address) {
            activeHost.value = hostList.value?.firstOrNull()
        }
    }

    val settings:Settings
        get() = Settings(
                activeHost = activeHost.value,
                hostList = hostList.value ?: listOf(),
                sourceType = sourceType.value ?: SourceType.DB,
                rating = ratingGroup.rating ?: Rating.NORMAL,
                marks = markGroup.marks,
                category = category.value)

    fun load(context:Context) {
        val s = Settings.load(context)
        activeHost.value = s.activeHost
        hostList.value = s.hostList
        sourceType.value = s.sourceType
        ratingGroup.rating = s.rating
        markGroup.marks = s.marks
        category.value = s.category
    }

    fun save(context:Context):Boolean {
        val s = settings
        if(!s.isValid) {
            return false
        }
        s.save(context)
        return true
    }

    companion object {
        fun instanceFor(activity: ViewModelStoreOwner):SettingViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(SettingViewModel::class.java)
        }
    }
}

class HostItemViewModel(
    val address:String,
    val settingViewModel: SettingViewModel) : ViewModel() {

    val isActive = settingViewModel.activeHost.map {
        it == address
    }

    fun activate() {
        settingViewModel.activeHost.value = address
    }

    fun remove() {
        settingViewModel.removeHost(address)
    }
}