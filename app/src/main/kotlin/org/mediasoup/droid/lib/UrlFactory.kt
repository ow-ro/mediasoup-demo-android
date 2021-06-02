package org.mediasoup.droid.lib

object UrlFactory {
    private const val HOSTNAME = "v3demo.mediasoup.org"
    private const val TEST_HOSTNAME = "3.237.160.119"
    //private const val TEST_ROOM_ID = "9c0662e2-c817-447f-9a25-64df02265626"
    //private const val TEST_PEER_ID = "150415_98ae476b79ec402f9f4a438c855c0b12"

    //  private static final String HOSTNAME = "192.168.1.103";
    private const val PORT = 4443

    @JvmStatic
    fun getInvitationLink(roomId: String?, forceH264: Boolean, forceVP9: Boolean): String {
        return buildString {
            append("https://").append(TEST_HOSTNAME).append("/?roomId=").append(roomId)
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
            append("wss://").append(TEST_HOSTNAME).append(':').append(PORT).append("/?roomId=")
            append(roomId).append("&peerId=").append(peerId)
            if (forceH264) {
                append("&forceH264=true")
            } else if (forceVP9) {
                append("&forceVP9=true")
            }
        }
        //return "wss://3.237.160.119:4443/?roomId=11610c09-6e83-40cd-b8b7-178ac6926f1b&peerId=20033973_73eaa15b79544fe080c79e2f2c61894e"
    }
}