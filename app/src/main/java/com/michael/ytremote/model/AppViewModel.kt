package com.michael.ytremote.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.SimpleExoPlayer
import com.michael.ytremote.BooApplication
import com.michael.ytremote.data.VideoItem
import kotlinx.coroutines.launch
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
    var currentId:String? = null
    private lateinit var player: SimpleExoPlayer
    private var primaryOwner :WeakReference<IPlayerOwner>? = null
    private var secondaryOwner :WeakReference<IPlayerOwner>? = null

    fun registerPrimaryOwner(owner:IPlayerOwner) {
        primaryOwner?.get()?.ownerResigned()
        secondaryOwner?.get()?.ownerResigned()
        primaryOwner = WeakReference(owner.apply { ownerAssigned(player) })
    }

    fun getPlayer(owner:IPlayerOwner) {
        secondaryOwner?.get()?.ownerResigned()
        primaryOwner?.get()?.ownerResigned()
        secondaryOwner = WeakReference(owner.apply {ownerAssigned(player)})
    }

    fun releasePlayer(owner:IPlayerOwner) {
        if(owner==secondaryOwner?.get()) {
            owner.ownerResigned()
            secondaryOwner = null
            primaryOwner?.get()?.ownerAssigned(player)
        }
    }

    fun updateVideoList(retrieve:(suspend ()->List<VideoItem>?)) {
        viewModelScope.launch {
            if(loading.value?:false) {
                return@launch
            }
            loading.value = true
            val list = retrieve()
            if(list!=null) {
                videoList.value = list
            }
            loading.value = false
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
        player.release()
    }

    companion object {
        val instance: AppViewModel
            get() = ViewModelProvider(BooApplication.instance, ViewModelProvider.NewInstanceFactory()).get(AppViewModel::class.java).apply {
                if(null==player) {
                    player = SimpleExoPlayer.Builder(BooApplication.instance).build()
                }
            }
    }
}