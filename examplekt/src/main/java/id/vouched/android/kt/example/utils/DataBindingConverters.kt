package id.vouched.android.kt.example.utils

import androidx.databinding.InverseMethod

object DataBindingConverters {
    @InverseMethod("stringToLong")
    @JvmStatic fun longToString(
        value: Long
    ): String = "$value"

    @JvmStatic fun stringToLong(
        value: String
    ): Long = value.toLongOrNull() ?: 0L
}
