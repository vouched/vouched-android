package id.vouched.android.kt.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import id.vouched.android.kt.example.databinding.DialogFragmentApiKeyFormBinding

class ApiKeyFormDialogFragment : DialogFragment() {

    companion object {
        const val REQUEST_API_KEY_DATA = "REQUEST_API_KEY_DATA"
        const val KEY_API_KEY = "KEY_API_KEY"
        const val KEY_GROUP_ID = "GROUP_ID"
    }

    private lateinit var binding: DialogFragmentApiKeyFormBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DialogFragmentApiKeyFormBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            binding = this
            isCancelable = false
            setupBinding()
        }.root
    }

    private fun setupBinding() {
        binding.groupIdTextInputLayout.visibility = if (BuildConfig.DEBUG) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.acceptButton.setOnClickListener {
            val apiKey = binding.apiKeyTextView.text.toString()
            val groupId = binding.groupIdTextView.text.toString()
            if (apiKey.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    R.string.dialog_fragment_api_key_error_message,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                findNavController().popBackStack()
                setFragmentResult(
                    REQUEST_API_KEY_DATA,
                    bundleOf(
                        KEY_API_KEY to apiKey
                    ).apply {
                        if (groupId.isNotBlank()) {
                            this.putString(KEY_GROUP_ID, groupId)
                        }
                    }
                )
            }
        }
    }
}
