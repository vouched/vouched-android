package id.vouched.android.kt.example.utils

import androidx.lifecycle.Observer

class EventWrapperObserver<T>(private val onEventUnhandledContent: (T) -> Unit) :
    Observer<EventWrapper<T>> {
    override fun onChanged(event: EventWrapper<T>?) {
        event?.getContentIfNotHandled()?.let { value ->
            onEventUnhandledContent(value)
        }
    }
}
