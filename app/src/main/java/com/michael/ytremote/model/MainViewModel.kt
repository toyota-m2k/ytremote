package com.michael.ytremote.model

import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.michael.ytremote.data.*

class MainViewModel : ViewModel(), IPlayerOwner {
//    val rating = MutableLiveData<Rating>()
//    val mark = MutableLiveData<Mark>()
//    val category = MutableLiveData<String>()
//    var settings:Settings? = null
    val showSidePanel = MutableLiveData(true)

    val appViewModel = AppViewModel.instance.apply { addRef() }
//    val busy : MutableLiveData<Boolean>
//        get() = appViewModel.loading
    val videoList : MutableLiveData<List<VideoItem>>
        get() = appViewModel.videoList

//    var currentHost:String? = null

//    val currentVideo : MutableLiveData<VideoItem>
//        get() = appViewModel.currentVideo
//    var currentId:String? = null

    val player = MutableLiveData<SimpleExoPlayer>()
    val hasPlayer = player.map { it != null }
    val playOnMainPlayer = hasPlayer.combineLatest(appViewModel.playing, appViewModel.videoList) {hasPlayer, playing, list->
        hasPlayer == true && playing == true && list != null
    }

    init {
        appViewModel.attachPrimaryOwner(this)
    }

    override fun ownerAssigned(player: SimpleExoPlayer) {
        this.player.value = player
    }

    override fun ownerResigned() {
        this.player.value = null
    }

//    val filter:VideoItemFilter
//        get() = settings?.run { VideoItemFilter(rating=rating, marks=marks, category = category) } ?: VideoItemFilter()

    fun update() {
        appViewModel.updateVideoList()
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