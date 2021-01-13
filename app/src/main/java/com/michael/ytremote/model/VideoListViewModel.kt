package com.michael.ytremote.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
}