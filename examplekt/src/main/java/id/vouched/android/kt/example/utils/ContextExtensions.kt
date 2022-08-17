package id.vouched.android.kt.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Context.checkSelfPermissionCompat(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.checkSelfPermissionsCompat(permissions: Array<String>): Map<String, Boolean> =
    permissions.associateWith { checkSelfPermissionCompat(it) }

fun Context.copyToClipboard(label: String, text: String) {
    (ContextCompat.getSystemService(this, ClipboardManager::class.java))?.let {
        val clip = ClipData.newPlainText(label, text)
        it.setPrimaryClip(clip)
    }
}
