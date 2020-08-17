package org.mediasoup.droid.lib.model

import org.json.JSONObject

class Peer(info: JSONObject) : Info() {
    override val id: String = info.optString("id")
    override var displayName: String = info.optString("displayName")
    override val device: DeviceInfo = info.optJSONObject("device")?.let {
        DeviceInfo(
            flag = it.optString("flag"),
            name = it.optString("name"),
            version = it.optString("version")
        )
    } ?: DeviceInfo.unknownDevice()

    internal val consumers = hashSetOf<String>()
    internal val dataConsumers = hashSetOf<String>()
}
