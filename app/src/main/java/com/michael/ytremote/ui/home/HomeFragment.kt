package com.michael.ytremote.ui.home

import android.content.Intent
import android.os.Build
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

//    private val videoUrl:String?
//        get() {
//            val id = viewModel.currentVideo.value?.id ?: return null
//            return HostInfo.videoUrl(id)
//        }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        viewModel = MainViewModel.instanceFor(requireActivity())
        binding = DataBindingUtil.inflate<FragmentHomeBinding>(inflater, R.layout.fragment_home, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }

//        homeViewModel =
//                ViewModelProvider(this).get(HomeViewModel::class.java)
//        val root = inflater.inflate(R.layout.fragment_home, container, false)
//        val textView: TextView = root.findViewById(R.id.text_home)
//        homeViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

//        viewModel.currentVideo.observe(viewLifecycleOwner) {
//            videoUrl?.let { url ->
//                binding.playerView.url = url
//            }
//        }

        viewModel.player.observe(requireActivity()) {
            binding.playerView.setPlayer(it)
        }

//        binding.playerView.endReachedListener.add("homeFragment") {
//            CoroutineScope(Dispatchers.Main).launch {
//                viewModel.appViewModel.nextVideo()
//            }
//        }

//        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_exo_prev)?.apply {
//            visibility = View.VISIBLE
//            setOnClickListener { viewModel.appViewModel.prevVideo() }
//        }
//        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_exo_next)?.apply {
//            visibility = View.VISIBLE
//            setOnClickListener { viewModel.appViewModel.nextVideo() }
//        }
        if(FullscreenVideoActivity.supportPinP) {
            binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_pinp_button)?.apply {
                visibility = View.VISIBLE
                setOnClickListener { showFullScreenViewer(true) }
            }
        }

//        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_full_button)?.apply {
//            visibility = View.VISIBLE
//            setOnClickListener{ showFullScreenViewer(false) }
//        }

        return binding.root
    }

    private fun showFullScreenViewer(pinp:Boolean) {
        val activity = requireActivity()
        val player = binding.playerView

        val intent = Intent(activity, FullscreenVideoActivity::class.java)
        intent.putExtra(FullscreenVideoActivity.KEY_PINP, pinp)
        if(pinp) {
            val vs = player.videoSize
            if(vs.width>0 && vs.height>0) {
                intent.putExtra(FullscreenVideoActivity.KEY_VIDEO_WIDTH, vs.width)
                intent.putExtra(FullscreenVideoActivity.KEY_VIDEO_HEIGHT, vs.height)
            }
        }
        activity.startActivity(intent)
    }


}