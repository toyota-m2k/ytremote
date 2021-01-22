package com.michael.ytremote.model

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.michael.ytremote.data.VideoItem
import com.michael.ytremote.player.MicClipping
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoItemViewModel(private val videoItem:VideoItem, private val listModel:MainViewModel) : ViewModel() {
    val name
        get() = videoItem.name
    val id
        get() = videoItem.id
    val start
        get() = videoItem.start
    val end
        get() = videoItem.end
    val clipping:MicClipping
        get() = videoItem.clipping

    val isSelected: LiveData<Boolean> = listModel.appViewModel.currentVideo.map {
        it?.id == videoItem.id
    }
    fun onSelected(view: View) {
        listModel.appViewModel.currentVideo.value = videoItem
        viewModelScope.launch {
            delay(500)
            listModel.resetSidePanel.value = null
        }
    }
}