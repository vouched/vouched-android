package id.vouched.android.kt.example

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import id.vouched.android.kt.example.utils.EventWrapper

class WelcomeFormViewModel : ViewModel() {

    sealed class Event {
        data class NavToNext(
            val firstName: String?,
            val lastName: String?,
            val allowCameraFlash: Boolean,
            val allowIdConfirmation: Boolean,
            val allowOrientationCheck: Boolean,
            val timeoutMilliseconds: Long
        ) : Event()
    }

    private val _event = MutableLiveData<EventWrapper<Event>>(null)

    val event: LiveData<EventWrapper<Event>> = _event

    val minTimeoutInMilliseconds = 60000L

    val firstName = MutableLiveData<String>()

    val lastName = MutableLiveData<String>()

    val allowCameraFlash = MutableLiveData(false)

    val allowIdConfirmation = MutableLiveData(true)

    val allowOrientationCheck = MutableLiveData(true)

    val allowTimeout = MutableLiveData(false)

    val timeoutMilliseconds = MutableLiveData(minTimeoutInMilliseconds)

    val showTimeoutError = MediatorLiveData<Boolean>().apply {
        val mediatorLogic: (
            timeoutEnabled: Boolean,
            timeoutMilliseconds: Long
        ) -> Boolean = { timeoutEnabled, timeoutMilliseconds ->
            timeoutEnabled && timeoutMilliseconds < minTimeoutInMilliseconds
        }
        addSource(timeoutMilliseconds) { timeout ->
            allowTimeout.value?.let { timeoutEnabled ->
                value = mediatorLogic(timeoutEnabled, timeout)
            }
        }
        addSource(allowTimeout) { timeoutEnabled ->
            timeoutMilliseconds.value?.let { timeout ->
                value = mediatorLogic(timeoutEnabled, timeout)
            }
        }
    }

    val startButtonEnabled = Transformations.map(showTimeoutError) {
        !it
    }

    fun startButtonAction() {
        if (showTimeoutError.value != true) {
            val timeout = if (allowTimeout.value == true) timeoutMilliseconds.value ?: 0L else -1L
            _event.value = EventWrapper(
                Event.NavToNext(
                    firstName.value ?: "",
                    lastName.value ?: "",
                    allowCameraFlash.value ?: false,
                    allowIdConfirmation.value ?: false,
                    allowOrientationCheck.value ?: false,
                    timeout
                )
            )
        }
    }
}
