package com.michael.ytremote.model

import androidx.lifecycle.*
import com.michael.ytremote.data.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val rating = MutableLiveData<Rating>()
    val mark = MutableLiveData<Mark>()
    val category = MutableLiveData<String>()
    val busy = MutableLiveData<Boolean>()
    val videoList = MutableLiveData<List<VideoItem>>()
    val currentVideo = MutableLiveData<VideoItem>()
    val resetSidePanel = MutableLiveData<Any>()
    val filter:VideoItemFilter
        get() = VideoItemFilter(rating=rating.value, mark=mark.value, category = category.value)

    fun update() {
        viewModelScope.launch {
            if(busy.value?:false) {
                return@launch
            }
            busy.value = true
            val list = VideoListSource.retrieve()
            if(list!=null) {
                videoList.value = list
            }
            busy.value = false
        }
    }

    companion object {
        fun instanceFor(activity: ViewModelStoreOwner):MainViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java)
        }
    }
}