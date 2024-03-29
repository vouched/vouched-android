package id.vouched.android.kt.example

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import id.vouched.android.FaceDetect
import id.vouched.android.FaceDetectOptions
import id.vouched.android.Instruction
import id.vouched.android.Step
import id.vouched.android.VouchedCameraHelper
import id.vouched.android.VouchedCameraHelperOptions
import id.vouched.android.VouchedLogger
import id.vouched.android.VouchedSession
import id.vouched.android.VouchedUtils
import id.vouched.android.exception.VouchedAssetsMissingException
import id.vouched.android.kt.example.databinding.FragmentFaceScanningBinding
import id.vouched.android.kt.example.utils.checkSelfPermissionsCompat
import id.vouched.android.kt.example.utils.launchAppDetailsSettings
import id.vouched.android.kt.example.utils.launchDelayed
import id.vouched.android.kt.example.utils.permissionsActivityResultLauncher
import id.vouched.android.liveness.LivenessMode
import id.vouched.android.model.Insight

class FaceScanningFragment : Fragment() {

    companion object {
        private val TAG = FaceScanningFragment::class.simpleName
    }

    private lateinit var binding: FragmentFaceScanningBinding

    private val navigationArgs: FaceScanningFragmentArgs by navArgs()

    /*
    * Vouched SDK components
    * */

    private lateinit var session: VouchedSession

    private lateinit var cameraHelper: VouchedCameraHelper

    /*
    * Uses permissions stuffs
    * */

    private var permissionAlreadyRequested = false // take care to request permissions from the user only once

    private val requiredPermissions = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.CAMERA
    )

    private val permissionsActivityResultLauncher = permissionsActivityResultLauncher {
        // this callback is executed once the user responds to a permission request
        permissionAlreadyRequested = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentFaceScanningBinding.inflate(
        inflater,
        container,
        false
    ).apply {
        setupBinding(this)
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVouchedSdk()
    }

    private fun setupBinding(binding: FragmentFaceScanningBinding) {
        binding.missingPermissionErrorMessageView.permissionsSettingsButton.setOnClickListener {
            // navigates to the app settings, so the user can grant the required permissions
            requireActivity().launchAppDetailsSettings()
        }
        this.binding = binding
    }

    private fun setupVouchedSdk() {
        session = navigationArgs.session
        try {
            val mainExecutor = ContextCompat.getMainExecutor(requireContext())
            val options = VouchedCameraHelperOptions.Builder()
                .withFaceDetectOptions(
                    FaceDetectOptions.Builder()
                        .withLivenessMode(LivenessMode.MOUTH_MOVEMENT)
                        .build()
                ).withFaceDetectResultListener(faceDetectResultListener)
                .build()
            cameraHelper = VouchedCameraHelper(
                requireContext(),
                viewLifecycleOwner,
                mainExecutor,
                binding.previewView,
                VouchedCameraHelper.Mode.FACE,
                options
            )
        } catch (ex: VouchedAssetsMissingException) {
            Log.e(TAG, "there was a problem trying to load the assets files", ex)
        }
    }

    override fun onPause() {
        super.onPause()
        cameraHelper.onPause()
    }

    override fun onResume() {
        super.onResume()
        tryResumeCamera()
    }

    private fun tryResumeCamera() {
        val permissions: Map<String, Boolean> = checkSelfPermissionsCompat(requiredPermissions)
        if (permissions.containsValue(false)) {
            // if one of the required permissions is not granted, do not try to resume the camera
            if (!permissionAlreadyRequested) {
                // if already asked the user for permissions once, you should not try to ask them again
                permissionsActivityResultLauncher.launch(requiredPermissions)
            }
            binding.missingPermissionErrorMessageView.root.visibility = View.VISIBLE
            binding.instructionsView.visibility = View.GONE
        } else {
            // once all required permissions are granted, it is possible to resume the camera
            cameraHelper.onResume()
            binding.instructionsView.visibility = View.VISIBLE
            binding.missingPermissionErrorMessageView.root.visibility = View.GONE
        }
    }

    private val faceDetectResultListener = FaceDetect.OnDetectResultListener { faceDetectResult ->
        handleInstructionsMessage(faceDetectResult.instruction)
        if (faceDetectResult.step == Step.POSTABLE) {
            cameraHelper.onPause()
            showLoading(true)
            session.postFace(
                requireContext(),
                faceDetectResult,
                null,
                vouchedSessionResponseListener
            )
        }
    }

    private val vouchedSessionResponseListener = VouchedSession.OnJobResponseListener { jobResponse ->
        // this callback is executed once the VouchedSession has finished sending the face data
        showLoading(false)
        jobResponse.error?.let { error ->
            Log.e(TAG, "Error: ${error.message}")
            handleInstructionsMessage("An error occurred")
            launchDelayed(2000) { tryResumeCamera() }
            return@OnJobResponseListener
        }

        val job = jobResponse.job
        VouchedLogger.getInstance().info(job.toJson())
        val insights = VouchedUtils.extractInsights(job)
        if (insights.isNotEmpty()) {
            handleInstructionsMessage(insights.first())
            launchDelayed(5000) { tryResumeCamera() }
        } else {
            // Nav to Results
            Toast.makeText(
                requireContext(),
                "Facial data sent successfully",
                Toast.LENGTH_LONG
            ).show()
            navToResults()
        }
    }

    private fun navToResults() {
        val direction = FaceScanningFragmentDirections.actionFaceScanningFragmentToResultsFragment(
            session
        )
        findNavController().navigate(direction)
    }

    private fun handleInstructionsMessage(instruction: Instruction) {
        binding.instructionsTextView.text = when (instruction) {
            Instruction.BLINK_EYES -> "Slowly Blink"
            Instruction.MOVE_CLOSER -> "Move Closer"
            Instruction.MOVE_AWAY -> "Move Away"
            Instruction.LOOK_FORWARD -> "Look Forward"
            Instruction.CLOSE_MOUTH -> "Close Mouth"
            Instruction.OPEN_MOUTH -> "Open Mouth"
            Instruction.HOLD_STEADY -> "Hold Steady"
            Instruction.NO_FACE -> "Show Face"
            else -> ""
        }
    }

    private fun handleInstructionsMessage(insight: Insight) {
        binding.instructionsTextView.text = when (insight) {
            Insight.NON_GLARE -> "image has glare"
            Insight.QUALITY -> "image is blurry"
            Insight.BRIGHTNESS -> "image needs to be brighter"
            Insight.FACE -> "image is missing required visual markers"
            Insight.GLASSES -> "please take off your glasses"
            Insight.UNKNOWN -> null
            else -> null
        } ?: "Unknown Error"
    }

    private fun handleInstructionsMessage(message: String) {
        binding.instructionsTextView.text = message
    }

    private fun showLoading(show: Boolean) {
        binding.loadingProgressIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }
}
