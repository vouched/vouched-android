package id.vouched.android.kt.example.utils

class EventWrapper<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        if (hasBeenHandled) {
            return null
        }
        hasBeenHandled = true
        return content
    }

    fun peekContent(): T = content
}
