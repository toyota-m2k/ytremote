package io.github.toyota32k.ytremote.player

import android.util.Size
import androidx.lifecycle.*
import io.github.toyota32k.bindit.Command
import io.github.toyota32k.utils.Callback
import io.github.toyota32k.utils.Listeners
import io.github.toyota32k.utils.combineLatest
import io.github.toyota32k.ytremote.data.ChapterList
import io.github.toyota32k.ytremote.data.VideoItem
import io.github.toyota32k.ytremote.model.AppViewModel
import kotlinx.coroutines.launch

class PlayerStateModel(val appViewModel: AppViewModel) {
    var currentItem:MutableLiveData<VideoItem?> = MutableLiveData(null)
    val currentId get() = currentItem.value?.id

    val isPlaying = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>(null)
//    val hasErrorMessage = errorMessage.mapEx { !it.isNullOrEmpty() }
    val videoSize = MutableLiveData<Size>()
    val duration = MutableLiveData<Long>(0)
    var ended:Boolean = false
    val chapterSource = ChapterSource()

    val isPinP = MutableLiveData<Boolean>(false)
    val chapterSelected = Callback<String,Unit>()

//    val endEvent = Listeners<Boolean>()
    val updateButtonOnPinP = Listeners<Boolean>()

    val commandFullscreen = Command()
    val commandCloseFullscreen = Command()
    val commandPinP = Command()
    val commandNextVideo = Command()
    val commandPrevVideo = Command()
    val commandNextChapter = Command()
    val commandPrevChapter = Command()
    val commandTogglePlay = Command()

    data class ChapterInfo(val list:ChapterList, val duration:Long, val trimming:Range) {
        val disabledRanges:List<Range> by lazy {
            list.disabledRanges(trimming).toList()
        }
    }

    inner class ChapterSource {
        val chapterList = MutableLiveData<ChapterList?>(null)
        val ready = combineLatest(duration,chapterList) { d,c-> d!!>0L&&c!=null }.distinctUntilChanged()
        val chapterInfo = ready.map { if(it) createChapterInfo() else null }

        fun reset() {
            duration.value = 0L
            chapterList.value = null
        }
        fun load() {
            val id = currentId ?: return
            appViewModel.viewModelScope.launch {
                val cl = ChapterList.get(id) ?: return@launch
                if(cl.ownerId == currentId) {
                    chapterList.value = cl
                }
            }
        }

        private fun createChapterInfo():ChapterInfo? {
            val current = currentItem.value ?: return null
            val cl = chapterList.value
            val dur = duration.value
            if(dur!=null && dur>0L && cl!=null) {
                if(cl.ownerId == currentId) {
                    return ChapterInfo(cl, dur, current.clipping)
                }
            }
            return null
        }
    }

    enum class PlayerState {
        None,       // 初期状態
        Loading,
        Error,
        Playing,
        Paused
    }

    private val mPlayerState = MutableLiveData<PlayerState>(PlayerState.None)
    private var fPlayerState
        get() = mPlayerState.value
        private set(v) { mPlayerState.value = v }

    val playerState: LiveData<PlayerState> = mPlayerState
    fun setPlayerState(state: PlayerState):Boolean {
        when(state) {
            PlayerState.Loading -> {
                if(fPlayerState == PlayerState.None) {
                    fPlayerState = state
                    return true
                }
            }
            PlayerState.Playing -> {
                if(fPlayerState== PlayerState.Loading ||fPlayerState== PlayerState.Paused) {
                    fPlayerState = state
                    return true
                }
            }
            PlayerState.Paused -> {
                if(fPlayerState== PlayerState.Loading ||fPlayerState== PlayerState.Playing) {
                    fPlayerState = state
                    return true
                }
            }
            PlayerState.Error -> {
                if(fPlayerState==PlayerState.Loading) {
                    fPlayerState = state
                    return true
                }
            }
            else-> {
                fPlayerState = state
                return true
            }
        }
        return false
    }
    val zeroSize = Size(0,0)

    fun onReset() {
        ended = false
        errorMessage.value = null
        chapterSource.reset()
        duration.value = 0L
        videoSize.value = zeroSize
        setPlayerState(PlayerState.None)
    }

    fun onLoading() {
        if(setPlayerState(PlayerState.Loading)) {
            ended = false
            errorMessage.value = null
            chapterSource.load()
            duration.value = 0L
            videoSize.value = zeroSize
        }
    }
    fun onLoaded(duration:Long, play:Boolean) {
        this.duration.value = duration
        if(play) {
            onPlay()
        } else {
            onPause()
        }
    }
    fun onError(msg:String?) {
        errorMessage.value = msg
        setPlayerState(PlayerState.Error)
    }
    fun onPlay() {
        setPlayerState(PlayerState.Playing)
    }
    fun onPause() {
        setPlayerState(PlayerState.Paused)
    }
    fun onEnd() {
        ended = fPlayerState == PlayerState.Playing
        setPlayerState(PlayerState.Paused)
        if(ended) {
            commandNextVideo.onClick(null)
        }
    }
}