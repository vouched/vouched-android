package id.vouched.android.kt.example.utils

import android.app.Dialog
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import id.vouched.android.kt.example.R

/*
* Handle a loading dialog that locks the UI of an [FragmentActivity]
* */

fun FragmentActivity.lockUiShowingLoadingDialog(message: String? = null) {
    if (getLoadingDialog()?.isShowing == true) {
        return
    }
    val dialog = MaterialAlertDialogBuilder(this).apply {
        setCancelable(false)
        message?.let {
            setMessage(it)
        }
        setView(buildCircularProgressIndicator())
    }.show().apply {
        findViewById<TextView>(android.R.id.message)?.apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }
    window.decorView.setTag(R.string.TAG_LOADING_UI, dialog)
}

fun FragmentActivity.dismissLoadingDialog() {
    getLoadingDialog()?.dismiss()
    window.decorView.setTag(R.string.TAG_LOADING_UI, null)
}

private fun FragmentActivity.getLoadingDialog(): Dialog? =
    window.decorView.getTag(R.string.TAG_LOADING_UI) as? Dialog

private fun FragmentActivity.buildCircularProgressIndicator(): View {
    val padding = resources.getDimension(R.dimen.size_s).toInt()
    val ctx = this
    return RelativeLayout(ctx).apply {
        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        setPadding(padding, padding, padding, padding)
        gravity = Gravity.CENTER_HORIZONTAL
        addView(
            CircularProgressIndicator(ctx).apply {
                this.isIndeterminate = true
            },
            lp
        )
    }
}

fun FragmentActivity.showCancelableMessage(
    message: String,
    onDismissListener: DialogInterface.OnDismissListener? = null
) {
    MaterialAlertDialogBuilder(this)
        .setMessage(message)
        .setCancelable(true)
        .setNeutralButton(
            R.string.accept
        ) { dialog, _ ->
            dialog.dismiss()
        }.apply {
            onDismissListener?.let {
                setOnDismissListener(it)
            }
        }.show()
}
