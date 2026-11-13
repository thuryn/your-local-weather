package org.thosp.yourlocalweather.events

import android.location.Address
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.CopyOnWriteArraySet

sealed interface AppEvent {
    data class MainWeatherUpdate(val result: String) : AppEvent
    data class ForecastWeatherUpdate(val result: String) : AppEvent
    data class GraphsWeatherUpdate(val result: String) : AppEvent
    data class AddressResolution(val address: Address?) : AppEvent
}

fun interface AppEventListener {
    fun onEvent(event: AppEvent)
}

object AppEventsBus {

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    private val listeners = CopyOnWriteArraySet<AppEventListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun registerListener(listener: AppEventListener) {
        listeners.add(listener)
    }

    @JvmStatic
    fun unregisterListener(listener: AppEventListener) {
        listeners.remove(listener)
    }

    @JvmStatic
    fun postMainWeatherUpdate(result: String) {
        postEvent(AppEvent.MainWeatherUpdate(result))
    }

    @JvmStatic
    fun postForecastWeatherUpdate(result: String) {
        postEvent(AppEvent.ForecastWeatherUpdate(result))
    }

    @JvmStatic
    fun postGraphsWeatherUpdate(result: String) {
        postEvent(AppEvent.GraphsWeatherUpdate(result))
    }

    @JvmStatic
    fun postAddressResolution(address: Address?) {
        postEvent(AppEvent.AddressResolution(address))
    }

    @JvmStatic
    fun postEvent(event: AppEvent) {
        _events.tryEmit(event)
        if (listeners.isNotEmpty()) {
            mainHandler.post {
                for (listener in listeners) {
                    listener.onEvent(event)
                }
            }
        }
    }
}
