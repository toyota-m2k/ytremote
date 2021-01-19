package com.michael.ytremote.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.michael.ytremote.R
import com.michael.ytremote.data.HostInfo
import com.michael.ytremote.databinding.FragmentHomeBinding
import com.michael.ytremote.model.VideoListViewModel
import com.michael.ytremote.player.FullscreenVideoActivity
import com.michael.ytremote.player.MicVideoPlayer
import com.michael.ytremote.utils.readBool
import com.michael.ytremote.utils.writeBool

class HomeFragment : Fragment() {
    private lateinit var viewModel:VideoListViewModel
    private lateinit var binding:FragmentHomeBinding

    private val videoUrl:String?
        get() {
            val id = viewModel.currentVideo.value?.id ?: return null
            return HostInfo.videoUrl(id)
        }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        viewModel = VideoListViewModel.instanceFor(requireActivity())
        binding = DataBindingUtil.inflate<FragmentHomeBinding>(inflater, R.layout.fragment_home, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
            handler = Handlers()
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


    inner class Handlers {


        fun onNext(view:View) {
            val list = viewModel.videoList.value ?: return
            val index = list.indexOf(viewModel.currentVideo.value) + 1
            if(index<list.count()) {
                viewModel.currentVideo.value = list[index]
            }
        }

        fun onPrev(view:View) {
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