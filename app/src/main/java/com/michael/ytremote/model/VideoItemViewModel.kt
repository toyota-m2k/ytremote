package com.michael.ytremote.model

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.michael.ytremote.R
import com.michael.ytremote.data.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoItemViewModel(private val videoItem:VideoItem, private val listModel:VideoListViewModel) : ViewModel() {
    val name
        get() = videoItem.name
    val id
        get() = videoItem.id

    val isSelected: LiveData<Boolean> = listModel.currentVideo.map {
        it?.id == videoItem.id
    }
    fun onSelected(view: View) {
        listModel.currentVideo.value = videoItem
        viewModelScope.launch {
            delay(500)
            listModel.resetSidePanel.value = null
        }
    }
}