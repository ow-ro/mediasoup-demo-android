package org.mediasoup.droid.demo.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import org.mediasoup.droid.demo.R
import org.mediasoup.droid.demo.databinding.ViewMeBinding
import org.mediasoup.droid.demo.vm.MeProps
import org.mediasoup.droid.lib.PeerConnectionUtils
import org.mediasoup.droid.lib.RoomClient

class MeView : RelativeLayout {
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

    private lateinit var binding: ViewMeBinding

    private fun init(context: Context) {
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.view_me, this, true)
        binding.peerView.videoRenderer.init(PeerConnectionUtils.eglContext, null)
    }

    fun setProps(props: MeProps, roomClient: RoomClient) {
        // set view model.
        binding.peerView.peerViewProps = props

        // register click listener.
        binding.peerView.info.setOnClickListener {
            val showInfo = props.showInfo.get()
            props.showInfo.set(showInfo == null || !showInfo)
        }
        binding.peerView.meDisplayName.setOnEditorActionListener(
            OnEditorActionListener { textView: TextView, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    roomClient.changeDisplayName(textView.text.toString().trim { it <= ' ' })
                    return@OnEditorActionListener true
                }
                false
            }
        )
        binding.peerView.stats.setOnClickListener { }
        binding.peerView.videoRenderer.setZOrderMediaOverlay(true)

        // set view model.
        binding.meProps = props

        // register click listener.
        binding.mic.setOnClickListener {
            if (MeProps.DeviceState.ON == props.micState.get()) {
                roomClient.muteMic()
            } else {
                roomClient.unmuteMic()
            }
        }
        binding.cam.setOnClickListener {
            if (MeProps.DeviceState.ON == props.camState.get()) {
                roomClient.disableCam()
            } else {
                roomClient.enableCam()
            }
        }
        binding.changeCam.setOnClickListener { roomClient.changeCam() }
        binding.share.setOnClickListener {
            if (MeProps.DeviceState.ON == props.shareState.get()) {
                roomClient.disableShare()
            } else {
                roomClient.enableShare()
            }
        }
    }
}