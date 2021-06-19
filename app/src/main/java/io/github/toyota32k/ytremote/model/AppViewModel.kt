package io.github.toyota32k.ytremote.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.SimpleExoPlayer
import io.github.toyota32k.bindit.list.ObservableList
import io.github.toyota32k.utils.UtLogger
import io.github.toyota32k.ytremote.BooApplication
import io.github.toyota32k.ytremote.data.ChapterList
import io.github.toyota32k.ytremote.data.Settings
import io.github.toyota32k.ytremote.data.VideoItem
import io.github.toyota32k.ytremote.data.VideoListSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.schedule

interface IPlayerOwner {
    fun ownerResigned()
    fun ownerAssigned(player:SimpleExoPlayer)
}

class AppViewModel : ViewModel() {
    val refCount = MutableLiveData<Int>()
    val loading = MutableLiveData<Boolean>()
    var lastUpdate : Long = 0L
    val videoSources = ObservableList<VideoItem>()
    val currentVideo = MutableLiveData<VideoItem>()
    val chapterList = MutableLiveData<ChapterList?>()
    val playing = MutableLiveData<Boolean>()
    var currentId:String? = null

    private var player: SimpleExoPlayer? = null
    private var primaryOwner :WeakReference<IPlayerOwner>? = null
    private var secondaryOwner :WeakReference<IPlayerOwner>? = null
    private var updateTimerTask: TimerTask? = null
    private var mSettings: Settings? = null

    var settings: Settings
        get() = mSettings!!
        set(v) {
            if(v!=mSettings) {
                mSettings = v
                refreshVideoList()
            }
        }

    fun attachPrimaryOwner(owner:IPlayerOwner) {
        primaryOwner?.get()?.ownerResigned()
        secondaryOwner?.get()?.ownerResigned()
        primaryOwner = WeakReference(owner.apply { ownerAssigned(player!!) })
    }

    fun retainPlayer(owner:IPlayerOwner) {
        addRef()
        secondaryOwner?.get()?.ownerResigned()
        primaryOwner?.get()?.ownerResigned()
        secondaryOwner = WeakReference(owner.apply {ownerAssigned(player!!)})
    }

    fun releasePlayer(owner:IPlayerOwner) {
        if(owner==secondaryOwner?.get()) {
            owner.ownerResigned()
            secondaryOwner = null
            primaryOwner?.get()?.ownerAssigned(player!!)
        }
        release()
    }

    fun refreshVideoList() {
        UtLogger.debug("refreshVideoList")
        viewModelScope.launch {
            if(loading.value == true) {
                return@launch
            }
            loading.value = true
            val src = withContext(Dispatchers.Default) {
                VideoListSource.retrieve()
            }
            lastUpdate = src?.modifiedDate ?: 0L
            loading.value = false

            if(src!=null) {
                UtLogger.debug("refreshVideoList count=${src.list.count()}")
                videoSources.replace(src.list)
                if(updateTimerTask==null) {
                    updateTimerTask = Timer().run {
                        schedule(60000,60000) {
                            updateVideoList()
                        }
                    }
                }
            } else {
                UtLogger.debug("refreshVideoList empty")
                videoSources.clear()
                updateTimerTask?.cancel()
                updateTimerTask = null
            }
        }
    }

    private fun updateVideoList() {
        UtLogger.debug("updateVideoList")

        viewModelScope.launch {
            if(loading.value == true) {
                return@launch
            }
            if(lastUpdate==0L) {
                return@launch
            }

            loading.value = true
            val src = withContext(Dispatchers.Default) {
                VideoListSource.retrieve(lastUpdate)
            }
            loading.value = false
            if(src!=null&&src.list.isNotEmpty()) {
                UtLogger.debug("updateVideoList count=${src.list.count()}")
                videoSources.addAll(src.list)
            } else {
                UtLogger.debug("updateVideoList empty")
            }
        }
    }

    fun nextVideo() {
        val index = videoSources.indexOf(currentVideo.value) + 1
        if(index<videoSources.count()) {
            currentVideo.value = videoSources[index]
        }
    }

    fun prevVideo() {
        val index = videoSources.indexOf(currentVideo.value) - 1
        if(0<=index) {
            currentVideo.value = videoSources[index]
        }
    }

    fun tryStart(id:String) {
        val item = videoSources.find { it.id==id } ?: return
        currentVideo.value = item
    }

    fun addRef() {
        val v = refCount.value
        if(v==null) {
            refCount.value = 1
        } else {
            refCount.value = v+1
        }
    }

    fun release() {
        val v = refCount.value
        if(v==null) {
            refCount.value = 0
        } else {
            refCount.value = v-1
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
        updateTimerTask?.cancel()
        updateTimerTask = null
    }

    companion object {
        val instance: AppViewModel
            get() = ViewModelProvider(BooApplication.instance, ViewModelProvider.NewInstanceFactory()).get(AppViewModel::class.java).apply {
                if(null==player) {
                    player = SimpleExoPlayer.Builder(BooApplication.instance).build()
                }
                if(null==mSettings) {
                    settings = Settings.load(BooApplication.instance)
                }
            }
    }
}