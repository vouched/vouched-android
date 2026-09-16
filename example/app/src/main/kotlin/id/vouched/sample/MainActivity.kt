package id.vouched.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.vouched.sdk.VouchedSession
import id.vouched.sdk.capture.CaptureMethod
import id.vouched.sdk.capture.ClassicDocumentDetectionOptions
import id.vouched.sdk.capture.DocumentCaptureConfig
import id.vouched.sdk.capture.DocumentCaptureOptions
import id.vouched.sdk.capture.DocumentDetectionMode
import id.vouched.sdk.capture.face.FaceCaptureConfig
import id.vouched.sdk.capture.face.FaceDetectionMode
import id.vouched.sdk.models.AppConfig
import id.vouched.sdk.models.FlowType
import id.vouched.sdk.models.SessionParams

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionParams = SessionParams(apiKey = BuildConfig.VOUCHED_API_KEY)

        val appConfig =
            AppConfig(
                type = FlowType.IDV,
                sessionParams = sessionParams,
                documentCaptureConfig =
                DocumentCaptureConfig(
                    includeBackId = true,
                    detectionMode = DocumentDetectionMode.ENHANCED_WITH_FALLBACK,
                    classicDocumentDetectionOptions =
                    ClassicDocumentDetectionOptions(
                        captureMethod = CaptureMethod.BOTH,
                        enableDistanceCheck = true,
                    ),
                    options =
                    DocumentCaptureOptions(
                        userImageConfirmation = true,
                    ),
                ),
                faceCaptureConfig = FaceCaptureConfig(detectionMode = FaceDetectionMode.ENHANCED_WITH_FALLBACK),
            )

        // Create session
        val session = VouchedSession(appConfig)

        setContent {
            MainView(
                session = session,
                onFinish = {
                    finish()
                },
            )
        }
    }
}
