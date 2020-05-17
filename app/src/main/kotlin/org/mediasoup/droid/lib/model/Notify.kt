package org.mediasoup.droid.lib.model

import org.mediasoup.droid.lib.Utils

class Notify(
    val type: String,
    val text: String,
    timeout: Int = 0
) {
    val id: String = Utils.getRandomString(6).toLowerCase(java.util.Locale.ROOT)
    val timeout: Int

    init {
        if (timeout == 0) {
            when (this.type) {
                "info" -> this.timeout = 3000
                "error" -> this.timeout = 5000
                else -> this.timeout = 0
            }
        } else {
            this.timeout = timeout
        }
    }
}