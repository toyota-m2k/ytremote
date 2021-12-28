package io.github.toyota32k.ytremote.model

import androidx.lifecycle.*
import com.google.android.exoplayer2.ExoPlayer
import io.github.toyota32k.bindit.Command
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.combineLatest

class MainViewModel : ViewModel(), IPlayerOwner {
    val appViewModel = AppViewModel.instance
    val showSidePanel = MutableLiveData(true)
    val player = MutableLiveData<ExoPlayer>()     // ExoPlayer はActivityのライフサイクルに影響されないのでビューモデルに覚えておく。
    val isPlayingOnMainView = combineLatest(player, appViewModel.playerStateModel.isPlaying) { player, isPlaying-> player!=null && isPlaying==true }.distinctUntilChanged()
//    val hasPlayer = player.mapEx { it != null }

    init {
        appViewModel.attachPrimaryPlayerOwner(this)
    }

    override fun ownerAssigned(player: ExoPlayer) {
        this.player.value = player
    }

    override fun ownerResigned() {
        this.player.value = null
    }

//    fun refresh() {
//        appViewModel.refreshVideoList()
//    }

    override fun onCleared() {
        logger.debug()
        super.onCleared()
        appViewModel.detachPlayerOwner(this)
    }

    // region Commands
    val commandShowDrawer = Command()
    val commandHideDrawer = Command()
    val commandSetting = Command()
    val commandPushUrl = Command()
    val commandReloadList = Command()
    val commandSyncToHost = Command()
    val commandSyncFromHost = Command()
    // endregion

    companion object {
        fun instanceFor(activity: ViewModelStoreOwner):MainViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java)
        }
        val logger = UtLog("MainView", omissionNamespace = "io.github.toyota32k.ytremote")
    }
}