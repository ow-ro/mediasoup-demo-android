package org.mediasoup.droid.demo.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mediasoup.droid.lib.lv.RoomStore
import java.lang.reflect.InvocationTargetException

abstract class EdiasProps internal constructor(
    application: Application,
    val roomStore: RoomStore
) : AndroidViewModel(application) {

    abstract fun connect(lifecycleOwner: LifecycleOwner)

    /**
     * A creator is used to inject the product ID into the ViewModel
     *
     * This creator is to showcase how to inject dependencies into ViewModels. It's not actually
     * necessary in this case, as the product ID can be passed in a public method.
     */
    class Factory(private val mApplication: Application, private val mStore: RoomStore) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (EdiasProps::class.java.isAssignableFrom(modelClass)) {
                try {
                    modelClass
                        .getConstructor(Application::class.java, RoomStore::class.java)
                        .newInstance(mApplication, mStore)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InstantiationException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                }
            } else super.create(modelClass)
        }
    }
}