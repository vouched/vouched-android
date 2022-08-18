package id.vouched.android.kt.example.utils

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

/**
 * [TextInputLayout] Bindings Adapters
 */
@BindingAdapter("error")
fun error(textInputLayout: TextInputLayout, error: String?) {
    textInputLayout.error = error
}
