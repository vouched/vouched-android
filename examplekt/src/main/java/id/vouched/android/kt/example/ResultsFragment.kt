package id.vouched.android.kt.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import id.vouched.android.VouchedSession
import id.vouched.android.kt.example.databinding.FragmentResultsBinding
import id.vouched.android.kt.example.utils.copyToClipboard

class ResultsFragment : Fragment() {

    private lateinit var binding: FragmentResultsBinding

    private val navigationArgs: ResultsFragmentArgs by navArgs()

    private val session: VouchedSession by lazy { navigationArgs.session }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentResultsBinding.inflate(
        inflater,
        container,
        false
    ).apply {
        binding = this
        binding.resultsList.visibility = View.GONE
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        binding.loadingView.visibility = View.VISIBLE
        session.confirm(requireContext(), null) { jobResponse ->
            binding.loadingView.visibility = View.GONE
            jobResponse.error?.let {
                binding.resultsList.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
            } ?: run {
                val jobResult = jobResponse.job.result
                printResults(
                    resultId = jobResult.confidences.id >= 0.9,
                    resultSelfie = jobResult.confidences.selfie >= 0.9,
                    resultSuccess = jobResult.success,
                    resultName = "${jobResult.firstName} ${jobResult.lastName}",
                    resultNameMatch = jobResult.confidences.nameMatch >= 0.9,
                    resultFaceMatch = jobResult.confidences.faceMatch >= 0.9,
                    jobResponse.job.id
                )
                binding.resultsList.visibility = View.VISIBLE

                binding.jobIdToClipboardButton.setOnClickListener {
                    val successfullyMessage = getString(
                        R.string.fragment_results_job_id_copied_successfully_message
                    )
                    requireContext().copyToClipboard(
                        label = successfullyMessage,
                        jobResponse.job.id
                    )
                    Toast.makeText(requireContext(), successfullyMessage, Toast.LENGTH_LONG).show()
                }

                binding.reverificationFlowButton.setOnClickListener {
                    ResultsFragmentDirections.actionResultsFragmentToReverificationFragment(
                        jobResponse.job.id
                    ).let { direction ->
                        findNavController().navigate(direction)
                    }
                }
            }
        }
    }

    private fun printResults(
        resultId: Boolean,
        resultSelfie: Boolean,
        resultSuccess: Boolean,
        resultName: String,
        resultNameMatch: Boolean,
        resultFaceMatch: Boolean,
        jobId: String
    ) {
        binding.validIdTextView.text = "Valid ID - $resultId"
        binding.validIdIcon.setImageResource(
            if (resultId) R.drawable.check else R.drawable.x
        )

        binding.validSelfieTextView.text = "Valid Selfie - $resultSelfie"
        binding.validSelfieIcon.setImageResource(
            if (resultSelfie) R.drawable.check else R.drawable.x
        )

        binding.validMatchTextView.text = "Valid Match - $resultSuccess"
        binding.validMatchIcon.setImageResource(
            if (resultSuccess) R.drawable.check else R.drawable.x
        )

        binding.nameTextView.text = "Name - $resultName"
        binding.nameIcon.setImageResource(
            if (resultNameMatch) R.drawable.check else R.drawable.x
        )

        binding.faceMatchTextView.text = "Face Match - $resultFaceMatch"
        binding.faceMatchIcon.setImageResource(
            if (resultFaceMatch) R.drawable.check else R.drawable.x
        )

        binding.jobIdTextView.text = jobId
    }
}
