package org.mediasoup.droid.lib

import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import org.json.JSONObject
import org.mediasoup.droid.lib.socket.WebSocketTransport
import org.protoojs.droid.Peer
import org.protoojs.droid.ProtooException
import timber.log.Timber

class Protoo(transport: WebSocketTransport, listener: Listener) : Peer(transport, listener) {
    fun request(method: String): Observable<String> {
        return request(method, JSONObject())
    }

    fun request(method: String, generator: (JSONObject) -> Unit): Observable<String> {
        val req = JSONObject()
        generator(req)
        return request(method, req)
    }

    private fun request(method: String, data: JSONObject): Observable<String> {
        Timber.d("request(), method: %s", method)
        return Observable.create { emitter: ObservableEmitter<String> ->
            request(method, data, object : ClientRequestHandler {
                override fun resolve(data: String) {
                    if (!emitter.isDisposed) {
                        emitter.onNext(data)
                    }
                }

                override fun reject(error: Long, errorReason: String) {
                    if (!emitter.isDisposed) {
                        emitter.onError(ProtooException(error, errorReason))
                    }
                }
            })
        }
    }

    @WorkerThread
    @Throws(ProtooException::class)
    fun syncRequest(method: String): String {
        return syncRequest(method, JSONObject())
    }

    @WorkerThread
    @Throws(ProtooException::class)
    fun syncRequest(method: String, generator: (JSONObject) -> Unit): String {
        val req = JSONObject()
        generator(req)
        return syncRequest(method, req)
    }

    @WorkerThread
    @Throws(ProtooException::class)
    private fun syncRequest(method: String, data: JSONObject): String {
        Timber.d("syncRequest(), method: %s", method)
        return try {
            request(method, data).blockingFirst()
        } catch (throwable: Throwable) {
            throw ProtooException(-1, throwable.message)
        }
    }
}