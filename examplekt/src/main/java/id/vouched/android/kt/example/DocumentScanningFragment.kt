package id.vouched.android.kt.example

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import id.vouched.android.BarcodeDetect
import id.vouched.android.BarcodeResult
import id.vouched.android.CardDetect
import id.vouched.android.CardDetectOptions
import id.vouched.android.CardDetectResult
import id.vouched.android.Instruction
import id.vouched.android.Step
import id.vouched.android.VouchedCameraHelper
import id.vouched.android.VouchedCameraHelperOptions
import id.vouched.android.VouchedLogger
import id.vouched.android.VouchedSession
import id.vouched.android.VouchedSessionParameters
import id.vouched.android.VouchedUtils
import id.vouched.android.exception.VouchedAssetsMissingException
import id.vouched.android.kt.example.databinding.FragmentDocumentScanningBinding
import id.vouched.android.kt.example.databinding.IncludeDocumentManualCaptureViewBinding
import id.vouched.android.kt.example.databinding.IncludeIdConfirmationBinding
import id.vouched.android.kt.example.databinding.IncludeTimeoutViewBinding
import id.vouched.android.kt.example.utils.checkSelfPermissionsCompat
import id.vouched.android.kt.example.utils.dismissLoadingDialog
import id.vouched.android.kt.example.utils.launchAppDetailsSettings
import id.vouched.android.kt.example.utils.lockUiShowingLoadingDialog
import id.vouched.android.kt.example.utils.permissionsActivityResultLauncher
import id.vouched.android.kt.example.utils.showCancelableMessage
import id.vouched.android.model.GeoLocation
import id.vouched.android.model.Insight
import id.vouched.android.model.Params

class DocumentScanningFragment : Fragment() {

    companion object {
        private val TAG = DocumentScanningFragment::class.simpleName
    }

    private var currentInstructionAnimationResource: Int? = null

    private lateinit var binding: FragmentDocumentScanningBinding

    /*
    * Navigation argument handling
    * */

    private val navigationArgs: DocumentScanningFragmentArgs by navArgs()

    private val idConfirmationEnabled: Boolean by lazy { navigationArgs.allowIdConfirmation }

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

    /*
     * View that shows the captured photo (manually or by automatic detection) before being
     * processed (it contains a button to retry and another to confirm the image processing)
     * */
    private val idConfirmationBinding: IncludeIdConfirmationBinding by lazy {
        binding.idConfirmationView
    }

    /*
    * View showing a timeout message when trying to get a document image automatically (if that
    * feature is enabled)
    * */
    private val timeoutView: IncludeTimeoutViewBinding by lazy {
        binding.timeoutView
    }

    /*
    * View that shows a button to capture a photo of the document manually (this appears only when
    * the automatic capture timeout has expired and the user chooses to capture the photo manually)
    * */
    private val manualCaptureView: IncludeDocumentManualCaptureViewBinding by lazy {
        binding.documentManualCaptureView
    }

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var currentLocation: Location? = null

    private val geoLocationEnabled by lazy { navigationArgs.geoLocationEnabled }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentDocumentScanningBinding.inflate(
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

    private fun setupBinding(binding: FragmentDocumentScanningBinding) {
        binding.missingPermissionErrorMessageView.permissionsSettingsButton.setOnClickListener {
            // navigates to the app settings, so the user can grant the required permissions
            requireActivity().launchAppDetailsSettings()
        }
        this.binding = binding
    }

    private fun setupVouchedSdk() {
        session = VouchedSession(
            navigationArgs.apiKey,
            VouchedSessionParameters.Builder().apply {
                navigationArgs.groupId?.let { groupId ->
                    withGroupId(groupId)
                }
            }.build()
        )
        try {
            val mainExecutor = ContextCompat.getMainExecutor(requireContext())
            val optionsBuilder = VouchedCameraHelperOptions.Builder()
                .withCardDetectOptions(
                    CardDetectOptions.Builder()
                        .withEnableDistanceCheck(false)
                        .withEnhanceInfoExtraction(true)
                        .withEnableOrientationCheck(navigationArgs.allowOrientationCheck)
                        .build()
                ).withCameraFlashDisabled(navigationArgs.allowCameraFlash)
                .withCardDetectResultListener(vouchedCameraHelperListener)
                .withBarcodeDetectResultListener(vouchedCameraHelperListener)
            if (navigationArgs.timeoutMilliseconds > 0) {
                optionsBuilder.withTimeOut(navigationArgs.timeoutMilliseconds) {
                    showTimeoutView()
                }
            }
            cameraHelper = VouchedCameraHelper(
                requireContext(),
                viewLifecycleOwner,
                mainExecutor,
                binding.previewView,
                VouchedCameraHelper.Mode.ID,
                optionsBuilder.build()
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
        val permissionsToCheck = if (geoLocationEnabled) {
            requiredPermissions.plus(locationPermissions)
        } else {
            requiredPermissions
        }
        val permissions: Map<String, Boolean> = checkSelfPermissionsCompat(permissionsToCheck)
        val requiredPermissionsGuaranteed = !requiredPermissions.any { permission ->
            permissions[permission] == false
        }
        val locationPermissionsGuaranteed = !locationPermissions.any { permission ->
            permissions[permission] != null && permissions[permission] == false
        }

        if ((!requiredPermissionsGuaranteed || !locationPermissionsGuaranteed) && !permissionAlreadyRequested) {
            permissionsActivityResultLauncher.launch(permissionsToCheck)
        } else if (!requiredPermissionsGuaranteed) {
            binding.missingPermissionErrorMessageView.root.visibility = View.VISIBLE
            binding.instructionsView.visibility = View.GONE
        } else {
            cameraHelper.onResume()
            tryToGetLocation()
            binding.instructionsView.visibility = View.VISIBLE
            binding.missingPermissionErrorMessageView.root.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryToGetLocation() {
        if (!geoLocationEnabled) { return }
        if (!checkSelfPermissionsCompat(locationPermissions).containsValue(false)) {
            binding.locationTextView.text = "Getting geolocation data"
            requireContext().getSystemService<LocationManager>()?.let { locationManager ->
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    locationManager.getCurrentLocation(
                        LocationManager.GPS_PROVIDER,
                        null,
                        ContextCompat.getMainExecutor(requireContext())
                    ) { location ->
                        currentLocation = location
                        binding.locationTextView.text = "Lat: ${location.latitude} Lng: ${location.longitude}"
                    }
                } else {
                    locationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        { location ->
                            currentLocation = location
                            binding.locationTextView.text = "Lat: ${location.latitude} Lng: ${location.longitude}"
                        },
                        null
                    )
                }
            } ?: run {
                binding.locationTextView.text = "Unable to determine user location"
            }
        } else {
            binding.locationTextView.text = "Unable to determine user location"
        }
    }

    private val vouchedCameraHelperListener = object :
        CardDetect.OnDetectResultListener,
        BarcodeDetect.OnBarcodeResultListener {

        override fun onCardDetectResult(cardResult: CardDetectResult?) {
            // this callback is executed every frame parsed by a VouchedCameraHelper
            cardResult?.let {
                // until "step" equals POSTABLE, only user instruction texts need to be updated
                if (cardResult.step != Step.POSTABLE) {
                    handleInstructionsMessageForCardDetect(it.instruction)
                    return@onCardDetectResult
                }
                // when "step" equals POSTABLE it means that the image of the document has been
                // obtained then it is possible to send it through VoucherSession to be analyzed
                // or ask the user to confirm that the image is clear and sharp
                cameraHelper.onPause()
                if (idConfirmationEnabled) {
                    cardResult.imageBitmap?.let { image ->
                        showConfirmationView(image) {
                            postId(cardResult)
                        }
                    }
                } else {
                    postId(cardResult)
                }
            }
        }

        override fun onBarcodeResult(barcodeResult: BarcodeResult?) {
            // this callback is executed every frame parsed by a VouchedCameraHelper when it is looking for a barcode
            handleInstructionsForBarcodeResult(barcodeResult)
            barcodeResult?.let {
                cameraHelper.onPause()
                requireActivity().lockUiShowingLoadingDialog("Please wait. Processing image.")
                session.postBackId(
                    requireContext(),
                    barcodeResult,
                    Params.Builder(),
                    vouchedSessionResponseListener
                )
            }
        }
    }

    private val vouchedSessionResponseListener = VouchedSession.OnJobResponseListener { jobResponse ->
        // this callback is executed once VouchedSession has finished
        // parsing an image of a document
        requireActivity().dismissLoadingDialog()
        jobResponse.error?.let { error ->
            Log.e(TAG, "Error: ${error.message}")
            requireActivity().showCancelableMessage("An error occurred") {
                tryResumeCamera()
            }
            return@OnJobResponseListener
        }

        val job = jobResponse.job
        VouchedLogger.getInstance().info(job.toJson())
        val insights = VouchedUtils.extractInsights(job)
        if (insights.isNotEmpty()) {
            val errorMessage = getAMessageFromInsight(insights.first())
            requireActivity().showCancelableMessage(errorMessage) {
                tryResumeCamera()
            }
        } else {
            cameraHelper.updateDetectionModes(job.result)
            when (val nextMode = cameraHelper.nextMode) {
                VouchedCameraHelper.Mode.COMPLETED -> {
                    // document scan finished
                    navigateToFaceScanning()
                }
                VouchedCameraHelper.Mode.BARCODE, VouchedCameraHelper.Mode.ID_BACK -> {
                    // finds if the jobResponse requires processing the back of the document
                    // NOTE: This only processes the response if .withEnhanceInfoExtraction is set to true,
                    // otherwise, it will always return a next state of COMPLETED
                    cameraHelper.switchMode(nextMode)
                    requireActivity().showCancelableMessage(
                        "Turn ID card over to backside and lay on surface"
                    ) {
                        tryResumeCamera()
                        clearManualMode()
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * post an [CardDetectResult] through a [VouchedSession]
     * useful to send in case of an automatic document detection
     * */
    private fun postId(cardResult: CardDetectResult) {
        when (cameraHelper.currentMode) {
            VouchedCameraHelper.Mode.ID -> {
                requireActivity().lockUiShowingLoadingDialog("Please wait. Processing image.")
                session.postFrontId(
                    requireContext(),
                    cardResult,
                    getParamsBuilderWithInputData(),
                    vouchedSessionResponseListener
                )
            }
            VouchedCameraHelper.Mode.ID_BACK -> {
                requireActivity().lockUiShowingLoadingDialog("Please wait. Processing image.")
                session.postBackId(
                    requireContext(),
                    cardResult,
                    Params.Builder(),
                    vouchedSessionResponseListener
                )
            }
            else -> {}
        }
    }

    /**
     * post an [Bitmap] through a [VouchedSession]
     * useful to send in case of a manually captured photo of the document
     * */
    private fun postId(idImage: Bitmap) {
        when (cameraHelper.currentMode) {
            VouchedCameraHelper.Mode.ID -> {
                requireActivity().lockUiShowingLoadingDialog("Please wait. Processing image.")
                session.postFrontId(
                    requireContext(),
                    idImage,
                    getParamsBuilderWithInputData(),
                    vouchedSessionResponseListener
                )
            }
            VouchedCameraHelper.Mode.ID_BACK, VouchedCameraHelper.Mode.BARCODE -> {
                requireActivity().lockUiShowingLoadingDialog("Please wait. Processing image.")
                session.postBackId(
                    requireContext(),
                    idImage,
                    Params.Builder(),
                    vouchedSessionResponseListener
                )
            }
            else -> {}
        }
    }

    private fun getParamsBuilderWithInputData(): Params.Builder =
        Params.Builder()
            .withFirstName(navigationArgs.firstName)
            .withLastName(navigationArgs.lastName)
            .withBirthDate(navigationArgs.birthDate)
            .withEnableIPAddress(true)
            .withEnablePhysicalAddress(true)
            .withEnableDarkWeb(true)
            .withEnableCrossCheck(true)
            .withEnableAAMVA(true).apply {
                if (!geoLocationEnabled) { return@apply }
                currentLocation?.let {
                    withGeoLocation(GeoLocation(it.latitude, it.longitude, null))
                } ?: run {
                    withGeoLocation(GeoLocation(null, null, "Unable to determine user location"))
                }
            }

    private fun showConfirmationView(image: Bitmap, confirmAction: () -> Unit) {
        idConfirmationBinding.root.visibility = View.VISIBLE
        idConfirmationBinding.confirmationImageView.setImageBitmap(image)
        idConfirmationBinding.confirmButton.setOnClickListener {
            idConfirmationBinding.root.visibility = View.GONE
            confirmAction()
        }
        idConfirmationBinding.retryButton.setOnClickListener {
            idConfirmationBinding.root.visibility = View.GONE
            tryResumeCamera()
        }
    }

    /*
    * Print handling of instructions or dialog prompts for the UI
    * */

    private fun handleInstructionsMessageForCardDetect(instruction: Instruction) {
        val instructions: Pair<String, Int?> = when (instruction) {
            Instruction.HOLD_STEADY -> Pair("Hold Steady", null)
            Instruction.MOVE_CLOSER -> Pair("Move Closer", null)
            Instruction.MOVE_AWAY -> Pair("Move Away", null)
            Instruction.ONLY_ONE -> Pair("Multiple IDs", null)
            Instruction.ROTATE_TO_HORIZONTAL -> Pair(
                "Rotate to horizontal",
                R.raw.vertical_to_horizontal
            )
            Instruction.ROTATE_TO_VERTICAL -> Pair(
                "Rotate to vertical",
                R.raw.horizontal_to_vertical
            )
            Instruction.NO_CARD -> Pair("Show Id", null)
            else -> Pair("", null)
        }
        binding.instructionsTextView.text = instructions.first
        showInstructionAnimation(instructions.second)
    }

    private fun showInstructionAnimation(resourceId: Int?) {
        resourceId?.let { resId ->
            if (resId != currentInstructionAnimationResource) {
                binding.instructionsAnimationView.visibility = View.VISIBLE
                binding.instructionsAnimationView.cancelAnimation()
                binding.instructionsAnimationView.setAnimation(resId)
                binding.instructionsAnimationView.playAnimation()
                currentInstructionAnimationResource = resId
            }
        } ?: clearInstructionAnimation()
    }
    private fun clearInstructionAnimation() {
        currentInstructionAnimationResource = null
        binding.instructionsAnimationView.cancelAnimation()
        binding.instructionsAnimationView.visibility = View.GONE
    }
    private fun handleInstructionsForBarcodeResult(barcodeResult: BarcodeResult?) {
        binding.instructionsTextView.text = if (barcodeResult == null) {
            "Focus camera on back of ID"
        } else ""
    }

    private fun getAMessageFromInsight(insight: Insight): String {
        return when (insight) {
            Insight.NON_GLARE -> "image has glare"
            Insight.QUALITY -> "image is blurry"
            Insight.BRIGHTNESS -> "image needs to be brighter"
            Insight.FACE -> "image is missing required visual markers"
            Insight.GLASSES -> "please take off your glasses"
            Insight.ID_PHOTO -> "ID needs a valid photo"
            Insight.UNKNOWN -> null
            else -> null
        } ?: "Unknown Error"
    }

    private fun navigateToFaceScanning() {
        val direction = DocumentScanningFragmentDirections.actionDocumentScanningFragmentToFaceScanningFragment(
            session
        )
        findNavController().navigate(direction)
    }

    private fun showTimeoutView() {
        timeoutView.root.visibility = View.VISIBLE
        timeoutView.retryButton.setOnClickListener {
            timeoutView.root.visibility = View.GONE
            cameraHelper.clearAndRestartTimeout()
        }
        timeoutView.manuallyOptionButton.setOnClickListener {
            timeoutView.root.visibility = View.GONE
            switchToManualMode()
        }
    }

    private fun switchToManualMode() {
        manualCaptureView.root.visibility = View.VISIBLE
        manualCaptureView.captureButton.setOnClickListener {
            requireActivity().lockUiShowingLoadingDialog("Taking a photo..")
            cameraHelper.capturePhoto { photo ->
                cameraHelper.onPause()
                showConfirmationView(photo) {
                    postId(photo)
                }
                requireActivity().dismissLoadingDialog()
            }
        }
    }

    private fun clearManualMode() {
        cameraHelper.clearAndRestartTimeout()
        manualCaptureView.root.visibility = View.GONE
    }
}
