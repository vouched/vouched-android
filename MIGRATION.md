# Migrating from the Vouched Android SDK v1 to v2

SDK v2 is a full rewrite of the Vouched Android SDK. The integration model, API surface, distribution artifact, and minimum OS version have all changed. This guide walks a current v1 customer through the steps to upgrade.

If you are integrating Vouched for the first time, skip this page and go straight to the [README](README.md).

## What changed

| | v1 | v2 |
|---|---|---|
| Artifact | `id.vouched.android:vouched-sdk` | `id.vouched.android:vouched-sdk-android` |
| Integration | Manual camera (`VouchedCameraHelper`) + session management (`VouchedSession`) | Drop-in `VouchedFlow` Composable |
| UI toolkit | Your own Activities/Fragments with CameraX | Jetpack Compose flow driven by the SDK |
| Detection dependencies | Optional ML Kit barcode/face dependencies you add yourself | Bundled — no extra dependencies |
| Language | Java / Kotlin | Kotlin |
| Minimum Android | Per v1 release | 8.0 (API 26) |

## Step 1 — Update your Gradle dependencies

Remove the v1 artifact and the optional ML Kit dependencies v1 required for barcode and face detection:

```kotlin
// REMOVE from your app's build.gradle.kts
implementation("id.vouched.android:vouched-sdk:1.2.0")

// These are no longer needed — detection is bundled with v2:
implementation("com.google.mlkit:barcode-scanning:17.0.2")
implementation("com.google.mlkit:face-detection:16.1.4")
// (or the play-services variants: play-services-mlkit-barcode-scanning / play-services-mlkit-face-detection)
```

Add the v2 artifact, core library desugaring (required by v2), and the Compose dependencies the flow renders with:

```kotlin
plugins {
    // Required to compile the setContent { ... } composable in Step 3.
    // Keep the version in sync with your Kotlin version (see Requirements).
    alias(libs.plugins.composeCompiler) // org.jetbrains.kotlin.plugin.compose
}

dependencies {
    implementation("id.vouched.android:vouched-sdk-android:2.0.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // The SDK uses Jetpack Compose internally but does not expose it transitively
    // so declare the Compose artifacts your own code references.
    implementation("androidx.activity:activity-compose:1.12.2")
}
```

> If you don't use a Gradle version catalog, apply the plugin with its fully qualified id instead: `id("org.jetbrains.kotlin.plugin.compose") version "<your-kotlin-version>"`.

Enable desugaring, raise your minimum SDK version, and turn on Compose:

```kotlin
android {
    defaultConfig {
        minSdk = 26 // v2 requires Android 8.0+
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}
```

Make sure `mavenCentral()` is in your repositories (v2 is published to Maven Central, same as v1).

## Step 2 — Verify permissions

v2 needs the same camera and network permissions. If you already integrated v1, you likely have `CAMERA` and `INTERNET` declared; add `ACCESS_NETWORK_STATE` if it is missing:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

The SDK's default capture screens request the camera permission at runtime for you.

## Step 3 — Replace the manual capture flow with `VouchedFlow`

In v1 you constructed a `VouchedCameraHelper`, tracked its detection `Mode` (ID / BARCODE / ID_BACK / FACE), and posted each stage to the backend yourself through a `VouchedSession`:

```java
// v1 — you drove every step
VouchedCameraHelper cameraHelper = new VouchedCameraHelper(this, this,
        ContextCompat.getMainExecutor(this), previewView, VouchedCameraHelper.Mode.ID,
        new VouchedCameraHelperOptions.Builder()
                .withCardDetectOptions(new CardDetectOptions.Builder()
                        .withEnableDistanceCheck(true)
                        .withEnhanceInfoExtraction(true)
                        .build())
                .withCardDetectResultListener(this)
                .build());

VouchedSession session = new VouchedSession("PUBLIC_KEY");

// In onCardDetectResult you decided which endpoint to post to:
VouchedCameraHelper.Mode currentMode = cameraHelper.getCurrentMode();
if (currentMode.equals(VouchedCameraHelper.Mode.ID)) {
    session.postFrontId(this, cardDetectResult, new Params.Builder(), this);
} else if (currentMode.equals(VouchedCameraHelper.Mode.ID_BACK)) {
    session.postBackId(this, cardDetectResult, null, this);
}
// ...then postFace(...) and confirm(...), handling JobResponse at every step
```

In v2 the SDK owns the camera, detection, mode transitions, retries, and every call to the Vouched backend. You configure a session, build a flow, and render one Composable:

```kotlin
// v2 — the SDK drives the flow
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.vouched.sdk.VouchedSDK
import id.vouched.sdk.VouchedSession
import id.vouched.sdk.models.AppConfig
import id.vouched.sdk.models.FlowResult
import id.vouched.sdk.models.FlowType
import id.vouched.sdk.models.SessionParams
import id.vouched.sdk.ui.flow.VouchedFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appConfig = AppConfig(
            type = FlowType.IDV, // ID + face verification
            sessionParams = SessionParams(apiKey = "YOUR_VOUCHED_API_KEY")
        )
        val session = VouchedSession(appConfig)

        val flowBuilder = VouchedSDK
            .createFlow(appConfig)
            .onFlowComplete { result ->
                when (result) {
                    is FlowResult.Success -> { /* result.job */ }
                    is FlowResult.Cancelled -> { /* result.reason */ }
                    is FlowResult.Error -> { /* result.error, result.message */ }
                }
            }

        setContent {
            VouchedFlow(session = session, flowBuilder = flowBuilder)
        }
    }
}
```

There is no `VouchedCameraHelper` to construct, no per-stage posting, and no explicit initialization call — the SDK initializes itself and validates your API key when the flow starts.

> **Note:** v2 also defines a class named `VouchedSession`, but it is not the v1 session. It is an opaque handle the flow manages for you; it takes an `AppConfig` instead of a public key, and you no longer call `postFrontId` / `postFace` / `confirm` on it.

### Compose requirement

`VouchedFlow` is a Jetpack Compose screen, and the SDK does not expose Compose transitively — its Compose artifacts are declared with `implementation`. Your app therefore has to bring the Compose toolchain itself:

- Apply the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`). Without it the Kotlin compiler rejects the `@Composable` lambda passed to `setContent`.
- Set `buildFeatures { compose = true }` in your `android` block.
- Declare the Compose artifacts your code references, for example `androidx.activity:activity-compose` (see Step 1).

Your entry point should then be a `ComponentActivity` calling `setContent { ... }` (as above). If your app is View-based, host the flow in a `ComposeView` inside your existing layout; the same three Gradle changes still apply.

## Step 4 — Map your v1 options to v2 configuration

v1 spread its options across `VouchedCameraHelperOptions`, `CardDetectOptions`, and `FaceDetectOptions`. v2 consolidates them into two config objects on `AppConfig`: `documentCaptureConfig` and `faceCaptureConfig`.

| v1 | v2 |
|---|---|
| `VouchedCameraHelper.Mode.ID` / `ID_BACK` / `BARCODE` | `FlowType.ID` + `DocumentCaptureConfig(includeBackId = ..., includeBarcodeScanning = ...)` |
| `VouchedCameraHelper.Mode.FACE` | `FlowType.IDV`, `FlowType.SELFIE_VERIFICATION`, or `FlowType.REVERIFY` |
| `CardDetectOptions.withEnableDistanceCheck(bool)` | `ClassicDocumentDetectionOptions(enableDistanceCheck = bool)` |
| `CardDetectOptions.withEnhanceInfoExtraction(bool)` | No direct equivalent. In v1 it enabled PDF417 barcode reading during classic front-ID detection. In v2, `includeBarcodeScanning = true` reads the PDF417 — on the front with the ENHANCED_WITH_FALLBACK pipeline (single-side scan), on the back with the classic pipeline (it implies a back capture). The ENHANCED_WITH_FALLBACK pipeline (`detectionMode = ENHANCED_WITH_FALLBACK`, the default) is a new, separate detection; with `includeBackId = true` its full-document scanner reads every zone on both sides, including a front barcode when the document carries one |
| `CardDetectOptions.withEnableOrientationCheck(bool)` | Handled by the v2 capture UX; no direct flag |
| `withTimeOut(ms, timeoutListener)` | `DocumentCaptureTuning(timeoutMs = ...)` (document) / `FaceCaptureConfig(timeoutMs = ...)` (face) |
| `FaceDetectOptions.withLivenessMode(...)` | `FaceCaptureConfig(livenessMode = ...)` |
| `Params.Builder().withFirstName(...).withLastName(...)` | `JobParams` on `AppConfig` |
| Manual photo capture (`capturePhoto(...)`) | `CaptureMethod.UPLOAD` or `CaptureMethod.BOTH` (gallery capture) |

Liveness mode mapping:

| v1 `LivenessMode` | v2 `FaceLivenessMode` |
|---|---|
| `MOUTH_MOVEMENT` | `MOUTH_MOVEMENT` |
| `DISTANCE` | `DISTANCE` |
| `BLINKING` | Not available in v2 — contact your Vouched representative |
| `NONE` | Not available in v2; the default challenge is `STRAIGHT` |

v2 also adds `ORIENTATION` and `STRAIGHT` challenges.

### Example: v1 setup and its v2 equivalent

v1:

```java
new VouchedCameraHelperOptions.Builder()
        .withCardDetectOptions(new CardDetectOptions.Builder()
                .withEnableDistanceCheck(true)
                .withEnhanceInfoExtraction(true)
                .build())
        .withFaceDetectOptions(new FaceDetectOptions.Builder()
                .withLivenessMode(LivenessMode.DISTANCE)
                .build())
        .withTimeOut(30000, timeoutListener)
        .build();
```

v2:

```kotlin
import id.vouched.sdk.capture.CaptureMethod
import id.vouched.sdk.capture.ClassicDocumentDetectionOptions
import id.vouched.sdk.capture.DocumentCaptureConfig
import id.vouched.sdk.capture.DocumentCaptureTuning
import id.vouched.sdk.capture.DocumentDetectionMode
import id.vouched.sdk.capture.face.FaceCaptureConfig
import id.vouched.sdk.capture.face.FaceLivenessMode
import id.vouched.sdk.models.AppConfig
import id.vouched.sdk.models.FlowType
import id.vouched.sdk.models.JobParams
import id.vouched.sdk.models.SessionParams

val appConfig = AppConfig(
    type = FlowType.IDV,
    sessionParams = SessionParams(apiKey = "YOUR_VOUCHED_API_KEY"),
    jobParams = JobParams(                       // replaces Params.Builder
        firstName = "Jane",
        lastName = "Doe",
    ),
    documentCaptureConfig = DocumentCaptureConfig(
         // withEnhanceInfoExtraction has no direct equivalent: in v1 it read the PDF417 barcode
         // on the front during classic detection. v2's classic pipeline never reads the barcode
         // on the front; includeBarcodeScanning reads it on the back capture instead.
        includeBarcodeScanning = true,
        // (`detectionMode = ENHANCED_WITH_FALLBACK`) is a new, separate detection that may read
        // the barcode on the front depending on the document being detected
        detectionMode = DocumentDetectionMode.ENHANCED_WITH_FALLBACK,
        classicDocumentDetectionOptions = ClassicDocumentDetectionOptions(
            captureMethod = CaptureMethod.BOTH,
            enableDistanceCheck = true,                               // replaces withEnableDistanceCheck
            tuning = DocumentCaptureTuning(timeoutMs = 30_000),       // replaces withTimeOut
        ),
    ),
    faceCaptureConfig = FaceCaptureConfig(
        livenessMode = FaceLivenessMode.DISTANCE,                     // replaces withLivenessMode
        timeoutMs = 30_000,
    ),
)
```

## Step 5 — Move result handling to `onFlowComplete`

In v1 you implemented the `VouchedSession.OnJobResponseListener` callback (`onJobResponse`) for every stage and extracted insights with `VouchedUtils.extractInsights(job)`:

```java
// v1
@Override
public void onJobResponse(JobResponse response) {
    if (response.getError() != null) {
        // handle app/network/system errors
    } else {
        Job job = response.getJob();
        List<Insight> insights = VouchedUtils.extractInsights(job);
        // business and navigation logic based on Job data
    }
}
```

In v2 there is a single completion callback, and the terminal result is a sealed `FlowResult`:

```kotlin
// v2
val flowBuilder = VouchedSDK
    .createFlow(appConfig)
    .onError { error ->
        // Fires before the flow completes with FlowResult.Error — use for logging/tracking
        Log.e("Vouched", "Flow failed", error)
    }
    .onFlowComplete { result ->
        when (result) {
            is FlowResult.Success -> {
                val job = result.job                 // APIJobResponse
                println("Job id: ${job.id}")
                println("Success: ${job.result.success}")
                println("Face match confidence: ${job.result.confidences?.faceMatch}")
                println("ID confidence: ${job.result.confidences?.id}")
                // job.signals replaces VouchedUtils.extractInsights
                // job.result also exposes parsed fields: firstName, lastName, birthDate, etc.
            }
            is FlowResult.Cancelled -> {
                println("Cancelled: ${result.reason}")
            }
            is FlowResult.Error -> {
                println("Error: ${result.message}")
            }
        }
    }
```

Additional error behavior in v2:

- An invalid API key surfaces an `ApiKeyValidationException` via `onError`; the flow shows a retry screen during initialization.
- Initialization or job-creation failures show a retry screen before completing with `FlowResult.Error`.

## Step 6 — (Optional) restore v1 customizations

**Custom instruction screens.** In v1 you built your own pre-capture screens and navigation. In v2, inject Compose screens before the ID and face stages:

```kotlin
import id.vouched.sdk.ui.flow.customComposeScreen

val flowBuilder = VouchedSDK
    .createFlow(appConfig)
    .withIdStageInstructionsScreen(
        provider = customComposeScreen { onContinue ->
            IdInstructionScreen(onContinue = onContinue)
        }
    )
    .withFaceStageInstructionsScreen(
        provider = customComposeScreen { onContinue ->
            FaceInstructionScreen(onContinue = onContinue)
        }
    )
    .onFlowComplete { result -> /* ... */ }
```

**Stage tracking.** Replace any bookkeeping you did on `VouchedCameraHelper.getCurrentMode()` with:

```kotlin
.onStageChange { from, to ->
    Log.d("Vouched", "Stage changed: $from -> $to")
}
```

**Analytics consent.** v2 can send anonymous analytics that help Vouched diagnose verification failures in your integration:

```kotlin
// Before the flow starts
val flowBuilder = VouchedSDK
    .createFlow(appConfig)
    .withTrackingConsent(true)

// Or at runtime, e.g. from your privacy settings
Vouched.setTrackingConsent(true) // or false
```

## Where v1 concepts went

| v1 | v2 |
|---|---|
| `VouchedSession` + manual `postFrontId` / `postBackId` / `postFace` / `confirm` | Handled internally by `VouchedFlow` |
| `VouchedCameraHelper` + CameraX lifecycle management | Handled internally by `VouchedFlow` |
| `CardDetect`, `FaceDetect`, `BarcodeDetect` | Internal detection pipelines (`DocumentDetectionMode`, `FaceDetectionMode`) |
| `VouchedCameraHelper.Mode` transitions (`getNextMode` / `updateDetectionModes`) | Automatic — driven by `AppConfig.type`, your `DocumentCaptureConfig` flags (`includeBackId`, `includeBarcodeScanning`), and backend job results |
| `VouchedSession.OnJobResponseListener` | `.onFlowComplete` |
| `VouchedUtils.extractInsights(job)` | `job.signals` on `FlowResult.Success.job` |
| `Params.Builder` | `JobParams` on `AppConfig` |
| `VouchedSessionParameters.Builder().withToken(...)` | Not needed — the flow manages the job token. For re-verification, use `FlowType.REVERIFY` |
| `RetryableError` | `FlowResult.Error` + `onError` callback |
| `TimeoutListener` / `clearAndRestartTimeout()` / manual `capturePhoto(...)` | On timeout, v2 automatically lets the user take the photo manually |

## Behavior changes to be aware of

- **Minimum Android 8.0 (API 26).** If your app supports older versions, you must raise `minSdk` to 26 or stay on v1.
- **Jetpack Compose.** v2 renders its own Compose UI, but it does not expose Compose transitively. Your app must apply the Compose compiler plugin, enable `buildFeatures { compose = true }`, and declare the Compose artifacts it references (for example `androidx.activity:activity-compose`) — see Step 1. With that in place, your entry point only needs a `ComponentActivity` (or a `ComposeView` in a View-based app).
- **Enhanced detection is licensed per application ID.** Enhanced document/face detection is faster and more reliable than the classic pipeline, and is enabled per app by Vouched. Give your Vouched representative every `applicationId` that ships (product flavors and `applicationIdSuffix` values each count separately). Until it is enabled, capture runs the classic pipeline — no code change is needed on your side once it is turned on.
- **Barcode scanning needs no extra dependency.** v1 required you to add ML Kit and juggle `Mode.BARCODE`. In v2, set `includeBarcodeScanning = true` on `DocumentCaptureConfig` and the PDF417 barcode is read as part of the back capture.
- **Timeouts degrade gracefully.** When a capture timeout expires, v2 falls back to manual photo capture instead of requiring a `TimeoutListener` + restart implementation.

## Legacy SDK

v1 remains available on Maven Central as `id.vouched.android:vouched-sdk`. The v1 source, example app, and documentation are preserved at the [v1.3.6 tag](https://github.com/vouched/vouched-android/tree/v1.3.6). New integrations and bug fixes target v2 only.

## Upgrade checklist

- [ ] Remove `id.vouched.android:vouched-sdk` and the ML Kit barcode/face dependencies
- [ ] Add `id.vouched.android:vouched-sdk-android` and core library desugaring
- [ ] Apply the Compose compiler plugin, enable `buildFeatures { compose = true }`, and add `androidx.activity:activity-compose`
- [ ] Raise `minSdk` to 26
- [ ] Declare `CAMERA`, `INTERNET`, and `ACCESS_NETWORK_STATE` permissions
- [ ] Replace `VouchedCameraHelper` + per-stage posting with `AppConfig` + `VouchedSession` + `VouchedFlow`
- [ ] Map capture options to `DocumentCaptureConfig` / `FaceCaptureConfig`
- [ ] Move `VouchedSession.OnJobResponseListener` logic into `.onFlowComplete`
- [ ] (Optional) Re-add custom instruction screens, `onStageChange`, `onError`, and tracking consent
- [ ] Register every shipping `applicationId` with Vouched to enable enhanced detection
- [ ] Test the full verification flow on a physical device

## Support

- Documentation: https://docs.vouched.id
- Contact: support@vouched.id
