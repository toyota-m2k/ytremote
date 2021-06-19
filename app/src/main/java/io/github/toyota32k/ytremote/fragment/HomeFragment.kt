package io.github.toyota32k.ytremote.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.bindit.Binder
import io.github.toyota32k.bindit.BoolConvert
import io.github.toyota32k.bindit.VisibilityBinding
import io.github.toyota32k.ytremote.R
import io.github.toyota32k.ytremote.model.MainViewModel
import io.github.toyota32k.ytremote.player.FullscreenVideoActivity
import io.github.toyota32k.ytremote.player.MicVideoPlayer

class HomeFragment : Fragment() {
    private lateinit var viewModel: MainViewModel

    inner class HomeViewBinder(owner:LifecycleOwner, view:View):Binder() {
        val playerView: MicVideoPlayer = view.findViewById(R.id.player_view)
        init {
            register(
                VisibilityBinding.create(owner, view.findViewById(R.id.unavailable_icon_view), viewModel.hasPlayer, BoolConvert.Inverse),
                viewModel.commandFullscreen.connectViewEx(view.findViewById(R.id.mic_ctr_full_button))
            )
            if(FullscreenVideoActivity.supportPinP) {
                view.findViewById<View>(R.id.mic_ctr_pinp_button).also {
                    register(viewModel.commandPinP.connectViewEx(it))
                    it.visibility = View.VISIBLE
                }
            }
        }
    }

    private lateinit var binder:HomeViewBinder

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        viewModel = MainViewModel.instanceFor(requireActivity())
        return inflater.inflate(R.layout.fragment_home, container, false).also { view->
            binder = HomeViewBinder(requireActivity(), view)
            viewModel.player.observe(requireActivity()) {
                binder.playerView.setPlayer(it)
            }
        }
    }


}