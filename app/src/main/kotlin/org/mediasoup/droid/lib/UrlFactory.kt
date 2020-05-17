package org.mediasoup.droid.lib

object UrlFactory {
    private const val HOSTNAME = "v3demo.mediasoup.org"

    //  private static final String HOSTNAME = "192.168.1.103";
    private const val PORT = 4443

    @JvmStatic
    fun getInvitationLink(roomId: String?, forceH264: Boolean, forceVP9: Boolean): String {
        return buildString {
            append("https://").append(HOSTNAME).append("/?roomId=").append(roomId)
            if (forceH264) {
                append("&forceH264=true")
            } else if (forceVP9) {
                append("&forceVP9=true")
            }
        }
    }

    @JvmStatic
    fun getProtooUrl(roomId: String, peerId: String, forceH264:  Boolean, forceVP9: Boolean): String {
        return buildString {
            append("wss://").append(HOSTNAME).append(':').append(PORT).append("/?roomId=")
            append(roomId).append("&peerId=").append(peerId)
            if (forceH264) {
                append("&forceH264=true")
            } else if (forceVP9) {
                append("&forceVP9=true")
            }
        }
    }
}