package com.michael.ytremote.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.michael.ytremote.data.VideoItem

class AppViewModel : ViewModel() {
    val videoList = MutableLiveData<List<VideoItem>>()
}