package org.mediasoup.droid.demo.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import org.mediasoup.droid.demo.R
import org.mediasoup.droid.demo.databinding.ViewPeerBinding
import org.mediasoup.droid.demo.vm.PeerProps
import org.mediasoup.droid.lib.PeerConnectionUtils
import org.mediasoup.droid.lib.RoomClient

class PeerView : RelativeLayout {
    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    private lateinit var binding: ViewPeerBinding
    private fun init(context: Context) {
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.view_peer, this, true)
        binding.peerView.videoRenderer.init(PeerConnectionUtils.eglContext, null)
    }

    fun setProps(props: PeerProps, roomClient: RoomClient?) {
        // set view model into included layout
        binding.peerView.peerViewProps = props

        // register click listener.
        binding.peerView.info.setOnClickListener {
            props.showInfo.set(props.showInfo.get() == false)
        }
        binding.peerView.stats.setOnClickListener { }

        // set view model
        binding.peerProps = props
    }
}