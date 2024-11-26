package id.vouched.android.kt.example.utils

import androidx.lifecycle.Observer

/**
 * An [Observer] for [EventWrapper]s, simplifying the pattern of checking if the [EventWrapper]'s content
 * has already been handled.
 *
 * [onEventUnhandledContent] is only called if the [EventWrapper]'s content has not been handled.
 */
class EventWrapperObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<EventWrapper<T>?> {
    override fun onChanged(event: EventWrapper<T>?) {
        event?.getContentIfNotHandled()?.let(onEventUnhandledContent)
    }
}
