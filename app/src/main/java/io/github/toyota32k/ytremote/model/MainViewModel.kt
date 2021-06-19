package io.github.toyota32k.ytremote.model

import androidx.lifecycle.*
import com.google.android.exoplayer2.SimpleExoPlayer
import io.github.toyota32k.bindit.Command
import io.github.toyota32k.bindit.list.ObservableList
import io.github.toyota32k.utils.combineLatest
import io.github.toyota32k.ytremote.data.VideoItem

class MainViewModel : ViewModel(), IPlayerOwner {
    val showSidePanel = MutableLiveData(true)
    val appViewModel = AppViewModel.instance.apply { addRef() }
    val videoSources: ObservableList<VideoItem>
        get() = appViewModel.videoSources
    val player = MutableLiveData<SimpleExoPlayer>()     // ExoPlayer はActivityのライフサイクルに影響されないのでビューモデルに覚えておく。
    val hasPlayer = player.map { it != null }
    val playOnMainPlayer = combineLatest(hasPlayer, appViewModel.playing) {hasPlayer, playing->
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

    // region Commands
    val commandShowDrawer = Command()
    val commandHideDrawer = Command()
    val commandSetting = Command()
    val commandPushUrl = Command()
    val commandReloadList = Command()
    val commandFullscreen = Command()
    val commandPinP = Command()
    // endregion

    companion object {
        fun instanceFor(activity: ViewModelStoreOwner):MainViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java)
        }
    }
}