package com.michael.ytremote.player

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.SimpleExoPlayer

class ExoPlayerHolder : ViewModel() {
    var player: SimpleExoPlayer? = null
    var url:String? = null

    fun checkAndGo(url:String, fn:(String)->Unit) {
        if(this.url!=url) {
            this.url = url
            fn(url)
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }

    companion object {
        fun instanceFor(activity: FragmentActivity): ExoPlayerHolder {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(ExoPlayerHolder::class.java).apply {
                if(player==null) {
                    player = SimpleExoPlayer.Builder(activity).build()
                }
            }
        }
    }

}