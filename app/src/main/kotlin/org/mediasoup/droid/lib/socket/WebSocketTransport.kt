package org.mediasoup.droid.lib.socket

import android.os.Handler
import android.os.HandlerThread
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import org.json.JSONObject
import org.protoojs.droid.Message
import org.protoojs.droid.transports.AbsWebSocketTransport
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import kotlin.math.pow

class WebSocketTransport(url: String?) : AbsWebSocketTransport(url) {
    // Closed flag.
    private var closed = false

    // Connected flag.
    private var connected = false

    // OKHttpClient.
    private val okHttpClient: OkHttpClient

    // Handler associate to current thread.
    private val handler: Handler

    // Retry operation.
    private val retryStrategy: RetryStrategy = RetryStrategy(10, 2, 1000, 8 * 1000)

    // WebSocket instance.
    private var webSocket: WebSocket? = null

    // Listener.
    private var listener: Listener? = null

    internal class RetryStrategy(
        private val retries: Int,
        private val factor: Int,
        private val minTimeout: Int,
        private val maxTimeout: Int
    ) {
        var retryCount = 1
        fun retried() {
            retryCount++
        }

        val reconnectInterval: Int
            get() {
                if (retryCount > retries) {
                    return -1
                }
                var reconnectInterval = (minTimeout * factor.toDouble().pow(retryCount.toDouble())).toInt()
                reconnectInterval = reconnectInterval.coerceAtMost(maxTimeout)
                return reconnectInterval
            }

        fun reset() {
            if (retryCount != 0) {
                retryCount = 0
            }
        }

    }

    override fun connect(listener: Listener) {
        Timber.d("connect()")
        this.listener = listener
        handler.post { newWebSocket() }
    }

    private fun newWebSocket() {
        webSocket = null
        okHttpClient.newWebSocket(
            Request.Builder().url(mUrl).addHeader("Sec-WebSocket-Protocol", "protoo").build(),
            ProtooWebSocketListener()
        )
    }

    private fun scheduleReconnect(): Boolean {
        val reconnectInterval = retryStrategy.reconnectInterval
        if (reconnectInterval == -1) {
            return false
        }
        Timber.d("scheduleReconnect() ")
        handler.postDelayed(
            {
                if (closed) {
                    return@postDelayed
                }
                Timber.w("doing reconnect job, retryCount: %d", retryStrategy.retryCount)
                okHttpClient.dispatcher.cancelAll()
                newWebSocket()
                retryStrategy.retried()
            },
            reconnectInterval.toLong()
        )
        return true
    }

    override fun sendMessage(message: JSONObject): String {
        check(!closed) { "transport closed" }
        val payload = message.toString()
        handler.post {
            if (closed) {
                return@post
            }
            webSocket?.send(payload)
        }
        return payload
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        Timber.d("close()")
        val countDownLatch = CountDownLatch(1)
        handler.post {
            webSocket?.close(1000, "bye")
            webSocket = null
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun isClosed(): Boolean {
        return closed
    }

    private inner class ProtooWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (closed) {
                return
            }
            Timber.d("onOpen() ")
            this@WebSocketTransport.webSocket = webSocket
            connected = true
            listener?.onOpen()
            retryStrategy.reset()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Timber.w("onClosed()")
            if (closed) {
                return
            }
            closed = true
            connected = false
            retryStrategy.reset()
            listener?.onClose()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Timber.w("onClosing()")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.w("onFailure()")
            if (closed) {
                return
            }
            if (scheduleReconnect()) {
                if (connected) {
                    listener?.onFail()
                } else {
                    listener?.onDisconnected()
                }
            } else {
                Timber.e("give up reconnect. notify closed")
                closed = true
                listener?.onClose()
                retryStrategy.reset()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Timber.d("onMessage()")
            if (closed) {
                return
            }
            val message = Message.parse(text) ?: return
            listener?.onMessage(message)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Timber.d("onMessage()")
        }
    }

    init {
        val logging = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Timber.i(message)
            }
        })
        okHttpClient = OkHttpClient.Builder().let {
            it.addInterceptor(logging)
            it.build()
        }
        val handlerThread = HandlerThread("socket")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }
}