package com.michael.ytremote.model

import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.michael.ytremote.bind.list.ObservableList
import com.michael.ytremote.data.*

class MainViewModel : ViewModel(), IPlayerOwner {
    val showSidePanel = MutableLiveData(true)

    val appViewModel = AppViewModel.instance.apply { addRef() }
//    val videoList : MutableLiveData<List<VideoItem>>
//        get() = appViewModel.videoList
    val videoSources: ObservableList<VideoItem>
        get() = appViewModel.videoSources

    val player = MutableLiveData<SimpleExoPlayer>()
    val hasPlayer = player.map { it != null }
    val playOnMainPlayer = hasPlayer.combineLatest(appViewModel.playing) {hasPlayer, playing->
        hasPlayer == true && playing == true
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

    fun refresh() {
        appViewModel.refreshVideoList()
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