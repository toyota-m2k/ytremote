package io.github.toyota32k.ytremote.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.SimpleExoPlayer
import io.github.toyota32k.bindit.Binder
import io.github.toyota32k.bindit.BoolConvert
import io.github.toyota32k.bindit.VisibilityBinding
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.disposableObserve
import io.github.toyota32k.ytremote.R
import io.github.toyota32k.ytremote.model.MainViewModel
import io.github.toyota32k.ytremote.player.FullscreenVideoActivity
import io.github.toyota32k.ytremote.player.MicVideoPlayer

class HomeFragment : Fragment() {
    companion object {
        val logger get() = MainViewModel.logger
    }

    private lateinit var viewModel: MainViewModel

    inner class HomeViewBinder(owner:LifecycleOwner, view:View):Binder() {
        private val playerView: MicVideoPlayer = view.findViewById(R.id.player_view)
        private val uavView:View = view.findViewById(R.id.unavailable_icon_view)
        init {
            register(
                viewModel.player.disposableObserve(owner) {
                    if(it!=null) {
                        playerView.bindPlayer(it, true, true, false)
                        uavView.visibility = View.GONE
                    } else {
                        playerView.unbindPlayer()
                        uavView.visibility = View.VISIBLE
                    }
                },
            )
        }

        override fun dispose() {
            super.dispose()
            playerView.unbindPlayer()
        }
    }

    private lateinit var binder:HomeViewBinder

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        logger.debug()
        viewModel = MainViewModel.instanceFor(requireActivity())
        return inflater.inflate(R.layout.fragment_home, container, false).also { view->
            binder = HomeViewBinder(requireActivity(), view)
        }
    }

    override fun onDestroyView() {
        logger.debug()
        binder.dispose()
        super.onDestroyView()
    }

}