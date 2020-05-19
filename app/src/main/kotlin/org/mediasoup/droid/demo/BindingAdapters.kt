package org.mediasoup.droid.demo

import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import io.github.zncmn.mediasoup.model.ConnectionState
import org.mediasoup.droid.demo.vm.MeProps.DeviceState
import org.mediasoup.droid.lib.model.DeviceInfo
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import timber.log.Timber
import java.util.*

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("edias_state", "edias_state_animation")
    fun roomState(view: ImageView, state: ConnectionState?, animation: Animation) {
        if (state == null) {
            return
        }
        when (state) {
            ConnectionState.CONNECTING -> {
                view.setImageResource(R.drawable.ic_state_connecting)
                view.startAnimation(animation)
            }
            ConnectionState.CONNECTED -> {
                view.setImageResource(R.drawable.ic_state_connected)
                animation.cancel()
                view.clearAnimation()
            }
            else -> {
                view.setImageResource(R.drawable.ic_state_new_close)
                animation.cancel()
                view.clearAnimation()
            }
        }
    }

    @JvmStatic
    @BindingAdapter("edias_link")
    fun inviteLink(view: TextView, inviteLink: String?) {
        view.visibility = if (inviteLink.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
    }

    @JvmStatic
    @BindingAdapter("edias_hide_videos", "edias_hide_videos_progress")
    fun hideVideos(view: ImageView, audioOnly: Boolean, audioOnlyInProgress: Boolean) {
        view.isEnabled = !audioOnlyInProgress
        if (audioOnly) {
            view.setBackgroundResource(R.drawable.bg_left_box_on)
            view.setImageResource(R.drawable.icon_video_black_on)
        } else {
            view.setBackgroundResource(R.drawable.bg_left_box_off)
            view.setImageResource(R.drawable.icon_video_white_off)
        }
    }

    @JvmStatic
    @BindingAdapter("edias_audio_muted")
    fun audioMuted(view: ImageView, audioMuted: Boolean) {
        if (audioMuted) {
            view.setBackgroundResource(R.drawable.bg_left_box_on)
            view.setImageResource(R.drawable.icon_volume_black_on)
        } else {
            view.setBackgroundResource(R.drawable.bg_left_box_off)
            view.setImageResource(R.drawable.icon_volume_white_off)
        }
    }

    @JvmStatic
    @BindingAdapter("edias_restart_ice_progress", "edias_restart_ice_ani")
    fun restartIce(view: ImageView, restart_ice_in_progress: Boolean, animation: Animation) {
        Timber.d("restartIce() %b", restart_ice_in_progress)
        view.isEnabled = !restart_ice_in_progress
        if (restart_ice_in_progress) {
            view.startAnimation(animation)
        } else {
            animation.cancel()
            view.clearAnimation()
        }
    }

    @JvmStatic
    @BindingAdapter("edias_device")
    fun deviceInfo(view: TextView, deviceInfo: DeviceInfo?) {
        if (deviceInfo == null) {
            return
        }
        var deviceIcon = R.drawable.ic_unknown
        val flag = deviceInfo.flag
        if (flag.isEmpty()) {
            view.text = ""
        } else {
            deviceIcon = when (flag.toLowerCase(Locale.ENGLISH)) {
                "chrome" -> R.mipmap.chrome
                "firefox" -> R.mipmap.firefox
                "safari" -> R.mipmap.safari
                "opera" -> R.mipmap.opera
                "edge" -> R.mipmap.edge
                "android" -> R.mipmap.android
                else -> R.drawable.ic_unknown
            }
            view.text = "${deviceInfo.name} ${deviceInfo.version}"
        }
        view.setCompoundDrawablesWithIntrinsicBounds(deviceIcon, 0, 0, 0)
    }

    @JvmStatic
    @BindingAdapter("edias_mic_state")
    fun deviceMicState(imageView: ImageView, state: DeviceState?) {
        if (state == null) {
            return
        }
        Timber.d("edias_mic_state: %s", state.name)
        if (DeviceState.ON == state) {
            imageView.setBackgroundResource(R.drawable.bg_media_box_on)
        } else {
            imageView.setBackgroundResource(R.drawable.bg_media_box_off)
        }
        when (state) {
            DeviceState.ON -> imageView.setImageResource(R.drawable.icon_mic_black_on)
            DeviceState.OFF -> imageView.setImageResource(R.drawable.icon_mic_white_off)
            DeviceState.UNSUPPORTED -> imageView.setImageResource(R.drawable.icon_mic_white_unsupported)
        }
    }

    @JvmStatic
    @BindingAdapter("edias_cam_state")
    fun deviceCamState(imageView: ImageView, state: DeviceState?) {
        if (state == null) {
            return
        }
        Timber.d("edias_cam_state: %s", state.name)
        if (DeviceState.ON == state) {
            imageView.setBackgroundResource(R.drawable.bg_media_box_on)
        } else {
            imageView.setBackgroundResource(R.drawable.bg_media_box_off)
        }
        when (state) {
            DeviceState.ON -> imageView.setImageResource(R.drawable.icon_webcam_black_on)
            DeviceState.OFF -> imageView.setImageResource(R.drawable.icon_webcam_white_off)
            DeviceState.UNSUPPORTED -> imageView.setImageResource(R.drawable.icon_webcam_white_unsupported)
        }
    }

    @JvmStatic
    @BindingAdapter("edias_change_came_state")
    fun changeCamState(view: View, state: DeviceState?) {
        if (state == null) {
            return
        }
        Timber.d("edias_change_came_state: %s", state.name)
        view.isEnabled = DeviceState.ON == state
    }

    @JvmStatic
    @BindingAdapter("edias_share_state")
    fun shareState(view: View, state: DeviceState?) {
        state?.also {
            Timber.d("edias_share_state: %s", it.name)
            view.isEnabled = DeviceState.ON == it
        }
    }

    @JvmStatic
    @BindingAdapter("edias_render")
    fun render(renderer: SurfaceViewRenderer, track: VideoTrack?) {
        Timber.d("edias_render: %b", track != null)
        if (track == null) {
            renderer.visibility = View.GONE
        } else {
            track.addSink(renderer)
            renderer.visibility = View.VISIBLE
        }
    }

    @JvmStatic
    @BindingAdapter("edias_render_empty")
    fun renderEmpty(renderer: View, track: VideoTrack?) {
        Timber.d("edias_render_empty: %b", track != null)
        if (track == null) {
            renderer.visibility = View.VISIBLE
        } else {
            renderer.visibility = View.GONE
        }
    }
}