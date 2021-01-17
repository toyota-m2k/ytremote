package com.michael.ytremote.model

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.michael.ytremote.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoListViewModel : ViewModel() {
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
        fun instanceFor(activity: ViewModelStoreOwner):VideoListViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(VideoListViewModel::class.java)
        }
    }
}