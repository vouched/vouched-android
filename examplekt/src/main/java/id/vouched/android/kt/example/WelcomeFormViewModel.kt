package id.vouched.android.kt.example

import androidx.lifecycle.*
import id.vouched.android.kt.example.utils.EventWrapper

class WelcomeFormViewModel : ViewModel() {

    sealed class Event {
        data class NavToNext(
            val firstName: String?,
            val lastName: String?,
            val allowCameraFlash: Boolean,
            val allowIdConfirmation: Boolean,
            val allowOrientationCheck: Boolean,
            val timeoutMilliseconds: Long,
            val allowGeolocation: Boolean
        ) : Event()
    }

    private val _event = MutableLiveData<EventWrapper<Event>>()
    val event: LiveData<EventWrapper<Event>> get() = _event

    companion object {
        private const val MIN_TIMEOUT_IN_MILLISECONDS = 60_000L
    }

    val minTimeoutInMilliseconds: LiveData<Long> = MutableLiveData(MIN_TIMEOUT_IN_MILLISECONDS)

    val firstName = MutableLiveData<String>("")
    val lastName = MutableLiveData<String>("")
    val allowCameraFlash = MutableLiveData(false)
    val allowIdConfirmation = MutableLiveData(true)
    val allowOrientationCheck = MutableLiveData(true)
    val allowTimeout = MutableLiveData(false)
    val allowGeoLocation = MutableLiveData(false)
    val timeoutMilliseconds = MutableLiveData(MIN_TIMEOUT_IN_MILLISECONDS)

    val showTimeoutError: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun updateErrorState() {
            val timeoutEnabled = allowTimeout.value ?: false
            val timeoutValue = timeoutMilliseconds.value ?: MIN_TIMEOUT_IN_MILLISECONDS
            value = timeoutEnabled && timeoutValue < MIN_TIMEOUT_IN_MILLISECONDS
        }

        addSource(timeoutMilliseconds) { updateErrorState() }
        addSource(allowTimeout) { updateErrorState() }
    }

    // Replace Transformations.map with the map extension function
    val startButtonEnabled: LiveData<Boolean> = showTimeoutError.map { !it }

    fun startButtonAction() {
        if (showTimeoutError.value == true) return

        val timeout = if (allowTimeout.value == true) {
            timeoutMilliseconds.value ?: MIN_TIMEOUT_IN_MILLISECONDS
        } else {
            -1L
        }

        _event.value = EventWrapper(
            Event.NavToNext(
                firstName = firstName.value.orEmpty(),
                lastName = lastName.value.orEmpty(),
                allowCameraFlash = allowCameraFlash.value ?: false,
                allowIdConfirmation = allowIdConfirmation.value ?: false,
                allowOrientationCheck = allowOrientationCheck.value ?: false,
                timeoutMilliseconds = timeout,
                allowGeolocation = allowGeoLocation.value ?: false
            )
        )
    }
}