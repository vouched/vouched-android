package id.vouched.android.kt.example.utils

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Fragment.checkSelfPermissionCompat(permission: String): Boolean =
    requireContext().checkSelfPermissionCompat(permission)

fun Fragment.checkSelfPermissionsCompat(permissions: Array<String>): Map<String, Boolean> =
    requireContext().checkSelfPermissionsCompat(permissions)

fun Fragment.permissionActivityResultLauncher(
    callBack: ActivityResultCallback<Boolean>
): ActivityResultLauncher<String> =
    registerForActivityResult(ActivityResultContracts.RequestPermission(), callBack)

fun Fragment.permissionsActivityResultLauncher(
    callBack: ActivityResultCallback<Map<String, Boolean>>
): ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), callBack)

fun Fragment.launchDelayed(delay: Long, action: () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
        delay(delay)
        launch(Dispatchers.Main) {
            action()
        }
    }
}
