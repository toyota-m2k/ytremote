package io.github.toyota32k.ytremote.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.SimpleExoPlayer
import io.github.toyota32k.bindit.list.ObservableList
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.ytremote.BooApplication
import io.github.toyota32k.ytremote.data.LastPlayInfo
import io.github.toyota32k.ytremote.data.Settings
import io.github.toyota32k.ytremote.data.VideoItem
import io.github.toyota32k.ytremote.data.VideoListSource
import io.github.toyota32k.ytremote.player.PlayerOwnerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.concurrent.schedule

interface IPlayerOwner {
    fun ownerResigned()
    fun ownerAssigned(player:SimpleExoPlayer)
}

class AppViewModel : ViewModel() {
    private val playerOwnerModel = PlayerOwnerManager(this)
    val playerStateModel = playerOwnerModel.playerState
    var refCount = RefCount()
    var lastPlayInfo:LastPlayInfo? = null

    // 通信中フラグ
    private val loading = MutableLiveData<Boolean>()
    private var lastUpdate : Long = 0L

    val videoSources = ObservableList<VideoItem>()
    val currentItem get() = playerStateModel.currentItem

    private var updateTimerTask: TimerTask? = null
    private var mSettings: Settings? = null

//    val playing = MutableLiveData<Boolean>()
//    var currentId:String? = null

    init {
        refCount.observeRelease {
            logger.debug("released... clear viewModelStore")
            BooApplication.instance.releaseViewModelStore()
        }
    }

    fun attachPrimaryPlayerOwner(owner:IPlayerOwner) {
        refCount++
        playerOwnerModel.attachPrimaryOwner(owner)
    }
    fun attachSecondaryPlayerOwner(owner:IPlayerOwner) {
        refCount++
        playerOwnerModel.attachSecondaryOwner(owner)
    }
    fun detachPlayerOwner(owner:IPlayerOwner) {
        refCount--
        playerOwnerModel.detachOwner(owner)
    }

    var settings: Settings
        get() = mSettings!!
        set(v) {
            if(v!=mSettings) {
                mSettings = v
                refreshVideoList()
            }
        }

    fun refreshVideoList() {
        logger.debug()
        viewModelScope.launch {
            if(loading.value == true) {
                logger.error("busy.")
                return@launch
            }
            val src = try {
                loading.value = true
                withContext(Dispatchers.Default) {
                    VideoListSource.retrieve()
                }
            } finally {
                loading.value = false
            }

            lastUpdate = src?.modifiedDate ?: 0L
            if(src!=null) {
                logger.debug("list.count=${src.list.count()}")
                videoSources.replace(src.list)
                if(src.list.isNotEmpty() && currentItem.value==null) {
                    val lpi = LastPlayInfo.get(BooApplication.instance)
                    if(lpi!=null) {
                        val item = src.list.find { it.id == lpi.id }
                        if(item!=null) {
                            currentItem.value = item
                            lastPlayInfo = lpi
                        }
                    }
                }
                if(updateTimerTask==null) {
                    updateTimerTask = Timer().run {
                        schedule(60000,60000) {
                            updateVideoList()
                        }
                    }
                }
            } else {
                logger.debug("list empty")
                videoSources.clear()
                updateTimerTask?.cancel()
                updateTimerTask = null
            }
        }
    }

    private fun updateVideoList() {
        logger.debug()

        viewModelScope.launch {
            if(loading.value == true) {
                logger.error("busy.")
                return@launch
            }
            if(lastUpdate==0L) {
                logger.debug("not necessary.")
                return@launch
            }

            loading.value = true
            val src = try {
                withContext(Dispatchers.Default) {
                    VideoListSource.retrieve(lastUpdate)
                }
            } finally {
                loading.value = false
            }
            if(src!=null&&src.list.isNotEmpty()) {
                logger.debug("list count=${src.list.count()}")
                videoSources.addAll(src.list)
            } else {
                logger.debug("list empty")
            }
        }
    }

    fun nextVideo() {
        val index = videoSources.indexOf(currentItem.value) + 1
        if(index<videoSources.count()) {
            currentItem.value = videoSources[index]
        }
    }

    fun prevVideo() {
        val index = videoSources.indexOf(currentItem.value) - 1
        if(0<=index) {
            currentItem.value = videoSources[index]
        }
    }

    fun tryStart(id:String) {
        val item = videoSources.find { it.id==id } ?: return
        currentItem.value = item
    }

    override fun onCleared() {
        logger.debug()
        LastPlayInfo.set(BooApplication.instance, currentItem.value?.id, playerOwnerModel.player?.currentPosition?:0, playerOwnerModel.player?.isPlaying?:false)
        super.onCleared()
        playerOwnerModel.closePlayer()
        updateTimerTask?.cancel()
        updateTimerTask = null
    }

    companion object {
        val logger = UtLog("APP")
        val instance: AppViewModel
            get() = ViewModelProvider(BooApplication.instance, ViewModelProvider.NewInstanceFactory()).get(AppViewModel::class.java).apply {
                playerOwnerModel.preparePlayer(BooApplication.instance)
                if(null==mSettings) {
                    settings = Settings.load(BooApplication.instance)
                }
            }
    }
}