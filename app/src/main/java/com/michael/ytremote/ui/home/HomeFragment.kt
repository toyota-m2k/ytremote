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
import com.michael.ytremote.databinding.FragmentHomeBinding
import com.michael.ytremote.model.MainViewModel
import com.michael.ytremote.player.FullscreenVideoActivity

class HomeFragment : Fragment() {
    private lateinit var viewModel:MainViewModel
    private lateinit var binding:FragmentHomeBinding

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

        viewModel.player.observe(requireActivity()) {
            binding.playerView.setPlayer(it)
        }

        if(FullscreenVideoActivity.supportPinP) {
            binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_pinp_button)?.apply {
                visibility = View.VISIBLE
                setOnClickListener { showFullScreenViewer(true) }
            }
        }

        binding.playerView.findViewById<ImageButton>(R.id.mic_ctr_full_button)?.apply {
            visibility = View.VISIBLE
            setOnClickListener{ showFullScreenViewer(false) }
        }

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