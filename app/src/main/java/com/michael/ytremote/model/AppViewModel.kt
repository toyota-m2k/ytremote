package com.michael.ytremote.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.SimpleExoPlayer
import com.michael.ytremote.BooApplication
import com.michael.ytremote.data.Settings
import com.michael.ytremote.data.VideoItem
import com.michael.ytremote.data.VideoListSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

interface IPlayerOwner {
    fun ownerResigned()
    fun ownerAssigned(player:SimpleExoPlayer)
}

class AppViewModel : ViewModel() {
    val refCount = MutableLiveData<Int>()
    val loading = MutableLiveData<Boolean>()
    val videoList = MutableLiveData<List<VideoItem>>()
    val currentVideo = MutableLiveData<VideoItem>()
    val playing = MutableLiveData<Boolean>()
    var currentId:String? = null
    private var player: SimpleExoPlayer? = null
    private var primaryOwner :WeakReference<IPlayerOwner>? = null
    private var secondaryOwner :WeakReference<IPlayerOwner>? = null

    private var mSettings: Settings? = null
    var settings: Settings
        get() = mSettings!!
        set(v) {
            if(v!=mSettings) {
                mSettings = v
                updateVideoList()
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

    fun updateVideoList() {
        viewModelScope.launch {
            if(loading.value == true) {
                return@launch
            }
            loading.value = true
            val list = withContext(Dispatchers.Default) {
                VideoListSource.retrieve()
            }
            if(list!=null) {
                videoList.value = list
            }
            loading.value = false
        }
    }

    fun nextVideo() {
        val list = videoList.value ?: return
        val index = list.indexOf(currentVideo.value) + 1
        if(index<list.count()) {
            currentVideo.value = list[index]
        }
    }

    fun prevVideo() {
        val list = videoList.value ?: return
        val index = list.indexOf(currentVideo.value) - 1
        if(0<=index) {
            currentVideo.value = list[index]
        }
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