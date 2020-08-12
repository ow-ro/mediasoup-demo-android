package org.mediasoup.droid.lib.model

import android.os.Build
import org.json.JSONObject
import org.mediasoup.droid.lib.JsonUtils
import org.webrtc.WebRtcVersion

class DeviceInfo(
    var flag: String,
    var name: String,
    var version: String
) {
    fun toJSONObject(): JSONObject {
        val deviceInfo = JSONObject()
        JsonUtils.jsonPut(deviceInfo, "flag", flag)
        JsonUtils.jsonPut(deviceInfo, "name", name)
        JsonUtils.jsonPut(deviceInfo, "version", version)
        return deviceInfo
    }

    companion object {
        @JvmStatic
        fun androidDevice() = DeviceInfo(
            flag = "android",
            name = "Android " + Build.DEVICE,
            version = WebRtcVersion.VERSION
        )

        @JvmStatic
        fun unknownDevice() = DeviceInfo(
            flag = "unknown",
            name = "unknown",
            version = "unknown"
        )
    }
}