package com.michael.ytremote.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.michael.ytremote.data.Mark
import com.michael.ytremote.data.Rating
import com.michael.ytremote.data.SourceType

class SettingViewModel : ViewModel() {
    val activeHost = MutableLiveData<String>()
    val hostList = MutableLiveData<List<String>>()
    val sourceType = MutableLiveData<SourceType>()
    val rating=MutableLiveData<Rating>()
    val mark=MutableLiveData<Mark>()
    val category=MutableLiveData<String>()
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
}