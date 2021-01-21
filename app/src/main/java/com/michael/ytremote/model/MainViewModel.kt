package com.michael.ytremote.model

import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.michael.ytremote.data.*

class MainViewModel : ViewModel(), IPlayerOwner {
    val rating = MutableLiveData<Rating>()
    val mark = MutableLiveData<Mark>()
    val category = MutableLiveData<String>()
    val resetSidePanel = MutableLiveData<Any>()

    val appViewModel = AppViewModel.instance.apply { addRef() }
    val busy : MutableLiveData<Boolean>
        get() = appViewModel.loading
    val videoList : MutableLiveData<List<VideoItem>>
        get() = appViewModel.videoList
//    val currentVideo : MutableLiveData<VideoItem>
//        get() = appViewModel.currentVideo
//    var currentId:String? = null

    val player = MutableLiveData<SimpleExoPlayer>()
    val hasPlayer = player.map { it != null }

    init {
        appViewModel.attachPrimaryOwner(this)
    }

    override fun ownerAssigned(player: SimpleExoPlayer) {
        this.player.value = player
    }

    override fun ownerResigned() {
        this.player.value = null
    }

    val filter:VideoItemFilter
        get() = VideoItemFilter(rating=rating.value, mark=mark.value, category = category.value)

    fun update() {
        appViewModel.updateVideoList {
            VideoListSource.retrieve(filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        appViewModel.release()
    }

    companion object {
        fun instanceFor(activity: ViewModelStoreOwner):MainViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java)
        }
    }
}