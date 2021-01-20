package com.michael.ytremote.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.michael.ytremote.R
import com.michael.ytremote.data.HostInfo
import com.michael.ytremote.databinding.FragmentHomeBinding
import com.michael.ytremote.model.MainViewModel
import com.michael.ytremote.player.FullscreenVideoActivity
import com.michael.ytremote.player.MicVideoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var viewModel:MainViewModel
    private lateinit var binding:FragmentHomeBinding
    private lateinit var handlers:Handlers

    private val videoUrl:String?
        get() {
            val id = viewModel.currentVideo.value?.id ?: return null
            return HostInfo.videoUrl(id)
        }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        viewModel = MainViewModel.instanceFor(requireActivity())
        handlers = Handlers()
        binding = DataBindingUtil.inflate<FragmentHomeBinding>(inflater, R.layout.fragment_home, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
            handler = handlers
        }

//        homeViewModel =
//                ViewModelProvider(this).get(HomeViewModel::class.java)
//        val root = inflater.inflate(R.layout.fragment_home, container, false)
//        val textView: TextView = root.findViewById(R.id.text_home)
//        homeViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

        viewModel.currentVideo.observe(viewLifecycleOwner) {
            videoUrl?.let { url ->
                binding.playerView.url = url
            }
        }

        binding.playerView.endReachedListener.add("homeFragment") {
            CoroutineScope(Dispatchers.Main).launch {
                handlers.onNext(null)
            }
        }

        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_exo_prev)?.apply {
            visibility = View.VISIBLE
            setOnClickListener(handlers::onPrev)
        }
        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_exo_next)?.apply {
            visibility = View.VISIBLE
            setOnClickListener(handlers::onNext)
        }
        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_pinp_button)?.apply {
            visibility = View.VISIBLE
            setOnClickListener(handlers::onPinP)
        }
        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_full_button)?.apply {
            visibility = View.VISIBLE
            setOnClickListener(handlers::onFullscreen)
        }

        return binding.root
    }

    private fun showFullScreenViewer(pinp:Boolean) {
        val activity = requireActivity()
        val source = videoUrl ?: return
        val player = binding.playerView

        val position = player.seekPosition
        val clipping = player.clip
        val playing = player.playerState == MicVideoPlayer.PlayerState.Playing
        val intent = Intent(activity, FullscreenVideoActivity::class.java)
        intent.putExtra(FullscreenVideoActivity.KEY_SOURCE, source)
        intent.putExtra(FullscreenVideoActivity.KEY_POSITION, position)
        intent.putExtra(FullscreenVideoActivity.KEY_PLAYING, playing)
        intent.putExtra(FullscreenVideoActivity.KEY_PINP, pinp)
        if(pinp) {
            val vs = player.videoSize
            if(vs.width>0 && vs.height>0) {
                intent.putExtra(FullscreenVideoActivity.KEY_VIDEO_WIDTH, vs.width)
                intent.putExtra(FullscreenVideoActivity.KEY_VIDEO_HEIGHT, vs.height)
            }
        }
        if(null!=clipping) {
            intent.putExtra(FullscreenVideoActivity.KEY_CLIP_START, clipping.start)
            intent.putExtra(FullscreenVideoActivity.KEY_CLIP_END, clipping.end)
        }
        activity.startActivity(intent)
        player.pause()
    }


    @Suppress("UNUSED_PARAMETER")
    inner class Handlers {


        fun onNext(view:View?) {
            val list = viewModel.videoList.value ?: return
            val index = list.indexOf(viewModel.currentVideo.value) + 1
            if(index<list.count()) {
                viewModel.currentVideo.value = list[index]
            }
        }

        fun onPrev(view:View?) {
            val list = viewModel.videoList.value ?: return
            val index = list.indexOf(viewModel.currentVideo.value) - 1
            if(0<=index) {
                viewModel.currentVideo.value = list[index]
            }
        }

        fun onPinP(view:View) {
            showFullScreenViewer(true)
        }
        fun onFullscreen(view:View) {
            showFullScreenViewer(false)
        }
    }
}