package id.vouched.android.kt.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import id.vouched.android.kt.example.databinding.DialogFragmentJobIdFormBinding

class JobIdFormDialogFragment : DialogFragment() {

    private lateinit var binding: DialogFragmentJobIdFormBinding

    private val navigationArgs: JobIdFormDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        bundle: Bundle?
    ): View {
        return DialogFragmentJobIdFormBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            binding = this
            setupBinding()
        }.root
    }

    private fun setupBinding() {
        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
        binding.acceptButton.setOnClickListener {
            val jobId = binding.jobIdTextView.text?.toString() ?: ""
            if (jobId.isNotBlank()) {
                navToReverification(jobId)
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.dialog_fragment_job_id_form_error_message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navToReverification(jobId: String) {
        JobIdFormDialogFragmentDirections.actionJobIdFormDialogFragmentToReverificationFragment(
            jobId,
            navigationArgs.apiKey,
            navigationArgs.groupId,
            null
        ).let { direction ->
            findNavController().navigate(direction)
        }
    }
}
