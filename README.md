# Vouched Android SDK

Identity verification SDK for Android. Drop-in flow that handles document capture, face capture with liveness, and submission to the Vouched backend.

## Prerequisites

- A [Vouched account](https://app.vouched.id) with an active API key. If you don't have one, contact your Vouched representative.
- *Optional, for enhanced ID capture:* enhanced document detection reads documents faster and more reliably than the classic pipeline, and is licensed per application. To enable it, give your Vouched representative the application ID of every app that will run the SDK. Product flavors and an `applicationIdSuffix` each produce a separate application ID, and every one that ships has to be registered (for example `com.example.myapp` and `com.example.myapp.staging`).

  Until it is enabled, document capture runs the classic pipeline instead. Capture still works, and no code change is needed on your side once enhanced capture is turned on.

## Requirements

- Android SDK 26+ (Android 8.0+)
- Compile SDK: 36 (Android 16)
- Target SDK: 36 (Android 16)
- Kotlin 2.2.21+
- Android Gradle Plugin 8.12.3+
- Core library desugaring enabled (see [Installation](#installation))
- Jetpack Compose enabled (see [Installation](#installation))
- A Vouched API key (contact your Vouched representative)

## Features

- Document capture (front, back, and full ID) with automatic detection
- Face capture with liveness verification
- Drop-in Jetpack Compose flow that drives the entire verification flow end to end
- Multiple flow types: ID only, full ID verification (ID + face), re-verification, and selfie verification
- Customizable result handling (success, cancelled, error)
- Optional custom instruction screens before ID and face stages
- Optional error and stage-change callbacks
- Built with Kotlin Multiplatform for maximum compatibility

## Installation

Apply the Compose compiler plugin. Kotlin 2.0+ requires it to compile any `@Composable` code, including the `setContent { ... }` block in the Quick Start. In your app's `build.gradle.kts` the `plugins {}` block comes first:

```kotlin
plugins {
    // Keep the version in sync with your Kotlin version.
    alias(libs.plugins.composeCompiler) // org.jetbrains.kotlin.plugin.compose
}
```

> If you don't use a Gradle version catalog, apply the plugin with its fully qualified id instead: `id("org.jetbrains.kotlin.plugin.compose") version "<your-kotlin-version>"`.

Add the SDK, core library desugaring, and the Compose dependency the flow renders with:

```kotlin
dependencies {
    implementation("id.vouched.android:vouched-sdk-android:2.0.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // The SDK uses Jetpack Compose internally but does not expose it transitively
    // so declare the Compose artifacts your own code references.
    implementation("androidx.activity:activity-compose:1.12.2")
}
```

The SDK requires [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring). Enable it, and enable Compose, in your app's `android` block:

```kotlin
android {
    buildFeatures {
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}
```

Make sure you have Maven Central in your repositories:

```kotlin
repositories {
    google()
    mavenCentral()
}
```

## Permissions

Add the required permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Request camera permission at runtime (Android 6.0+) using the standard Android runtime permission APIs. The SDK's default capture screens handle the runtime permission request for you.

## Quick Start

The SDK initializes itself automatically the first time a flow starts — no manual initialization call is required. You only need to configure the session and launch the flow.

### 1. Configure the Session and Launch the Flow

Use the Vouched Flow API in your Activity:

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.vouched.sdk.SessionImp
import id.vouched.sdk.VouchedSDK
import id.vouched.sdk.models.AppConfig
import id.vouched.sdk.models.FlowType
import id.vouched.sdk.models.SessionParams
import id.vouched.sdk.models.FlowResult
import id.vouched.sdk.ui.flow.VouchedFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure the session
        val appConfig = AppConfig(
            type = FlowType.IDV,
            sessionParams = SessionParams(
                apiKey = "YOUR_VOUCHED_API_KEY"
            )
        )

        // Create session
        val session = SessionImp(appConfig)

        // Build the flow
        val flowBuilder = VouchedSDK
            .createFlow(appConfig)
            .onFlowComplete { result ->
                when (result) {
                    is FlowResult.Success -> {
                        // Handle success: result.job contains verification details
                        println("Verification complete: ${result.job.id}")
                    }
                    is FlowResult.Cancelled -> {
                        // Handle cancellation
                        println("Cancelled: ${result.reason}")
                    }
                    is FlowResult.Error -> {
                        // Handle error
                        println("Error: ${result.message}")
                    }
                }
            }

        setContent {
            VouchedFlow(
                session = session,
                flowBuilder = flowBuilder
            )
        }
    }
}
```

### 2. (Optional) Custom instruction screens

Show your own Compose screens before the ID and face capture stages:

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
    .onFlowComplete { result ->
        // Handle result
    }
```

Example instruction screen:

```kotlin
@Composable
fun IdInstructionScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Please have your ID ready")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onContinue) {
            Text("Start ID Capture")
        }
    }
}
```

### 3. (Optional) Error and stage-change callbacks

In addition to `onFlowComplete`, the flow builder exposes callbacks for errors and stage transitions:

```kotlin
val flowBuilder = VouchedSDK
    .createFlow(appConfig)
    .onStageChange { from, to ->
        // Track user progress through the flow stages
        Log.d("Vouched", "Stage changed: $from -> $to")
    }
    .onError { error ->
        // Called before the flow completes with FlowResult.Error
        Log.e("Vouched", "Flow error", error)
    }
    .onFlowComplete { result ->
        // Handle result
    }
```

### 4. (Optional) Enable analytics tracking

We recommend enabling anonymous analytics. It lets Vouched diagnose verification failures in your integration, which is the fastest path to a resolution when you raise a support ticket:

```kotlin
val flowBuilder = VouchedSDK
    .createFlow(appConfig)
    .withTrackingConsent(true)
    .onFlowComplete { result -> /* ... */ }
```

### 5. (Optional) Update tracking consent at runtime

If you already know the flow's `trackingConsent` before starting it, use `withTrackingConsent` (above). To update consent after the flow has started (for example, from your app's privacy settings), use the global API:

```kotlin
import id.vouched.sdk.Vouched

// Call when the user grants or revokes consent in your privacy settings
Vouched.setTrackingConsent(true) // or false
```

## Flow Types

The SDK supports four flow types, configured via [`AppConfig.type`]:

| FlowType               | Description                                              | Stages                                      |
|------------------------|----------------------------------------------------------|---------------------------------------------|
| `ID`                   | ID document capture only                                 | ID → Done                                   |
| `IDV`                  | Full identity verification (ID + face with liveness)     | ID → Face → Done                            |
| `REVERIFY`             | Re-verification (face only)                              | Face → Done                                 |
| `SELFIE_VERIFICATION`  | Selfie verification only                                 | Face → Done                                 |

## Session Configuration

### SessionParams

[`SessionParams`] configures the API session:

```kotlin
val sessionParams = SessionParams(
    apiKey = "YOUR_VOUCHED_API_KEY",          // Required
    accountGroupSid = null,                    // Optional: account group identifier
    authJobId = null,                          // Optional: authentication job reference
    jobConfigId = null                         // Optional: backend job configuration
)
```

### JobParams (user data)

[`JobParams`] lets you pre-populate the user's data on the verification job. All fields are optional:

```kotlin
val appConfig = AppConfig(
    type = FlowType.IDV,
    sessionParams = SessionParams(apiKey = "YOUR_API_KEY"),
    jobParams = JobParams(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com",
        phone = "+15551234567",
        birthDate = "1990-01-01",
        enableIPAddress = true,                 // Collect the user's IP address
        enablePhysicalAddress = true,           // Collect the user's physical address
        enableDarkWeb = true,                   // Run a dark web data breach check
        enableCrossCheck = true,                // Cross-check provided PII against the ID
        enableDriversLicenseValidation = true   // Validate the driver's license number
    )
)
```

## Advanced Configuration

### Document Capture Options

```kotlin
import id.vouched.sdk.capture.CaptureMethod
import id.vouched.sdk.capture.ClassicDocumentDetectionOptions
import id.vouched.sdk.capture.DocumentCaptureConfig
import id.vouched.sdk.capture.DocumentCaptureOptions
import id.vouched.sdk.capture.DocumentCaptureTuning
import id.vouched.sdk.capture.DocumentDetectionMode

val appConfig = AppConfig(
    type = FlowType.IDV,
    sessionParams = SessionParams(apiKey = "YOUR_API_KEY"),
    documentCaptureConfig = DocumentCaptureConfig(
        detectionMode = DocumentDetectionMode.CLASSIC_ONLY,
        includeBackId = true,  // Capture both front and back
        // Options specific to classic (platform-native) document detection.
        classicDocumentDetectionOptions = ClassicDocumentDetectionOptions(
            captureMethod = CaptureMethod.BOTH,   // CAMERA, UPLOAD, or BOTH
            enableDistanceCheck = true,           // Front-ID distance challenge
            tuning = DocumentCaptureTuning(
                timeoutMs = 30_000,               // Capture timeout (null = no timeout)
                holdSteadyDurationMs = 1_000      // Hold time before auto-capture (ms)
            )
        ),
        // Capture-result acceptance options (confirmation, confidence gating).
        options = DocumentCaptureOptions(
            userImageConfirmation = true,
            cardIDThreshold = 0.8
        )
    )
)
```

- `detectionMode` chooses which detection pipeline runs (`ENHANCED_WITH_FALLBACK` or `CLASSIC_ONLY`). `ENHANCED_WITH_FALLBACK` tries enhanced detection first and falls back to classic detection if it is unavailable.
- `includeBackId` captures the back of the ID after the front.
- `classicDocumentDetectionOptions` configures the classic pipeline:
  - `captureMethod`: `CAMERA` (live capture), `UPLOAD` (gallery only), or `BOTH`.
  - `enableDistanceCheck`: runs the front-ID distance challenge (hold the ID farther away, then move closer).
  - `tuning` ([`DocumentCaptureTuning`]): `timeoutMs` and `holdSteadyDurationMs` (stability hold time).
- `options` ([`DocumentCaptureOptions`]) configures result acceptance:
  - `userImageConfirmation`: shows a confirmation screen so the user can review/retake the captured image.
  - `cardIDThreshold`: minimum backend ID confidence (0.0–1.0) required to progress; below it, the capture stage retries. `0.0` disables the gate.

### Face Capture Options

```kotlin
import id.vouched.sdk.capture.CaptureMethod
import id.vouched.sdk.capture.face.FaceCaptureConfig
import id.vouched.sdk.capture.face.FaceDetectionMode
import id.vouched.sdk.capture.face.FaceLivenessMode

val appConfig = AppConfig(
    type = FlowType.IDV,
    sessionParams = SessionParams(apiKey = "YOUR_API_KEY"),
    faceCaptureConfig = FaceCaptureConfig(
        detectionMode = FaceDetectionMode.ENHANCED_WITH_FALLBACK,
        timeoutMs = 30_000,                    // Capture timeout (null = no timeout)
        livenessMode = FaceLivenessMode.STRAIGHT,  // MOUTH_MOVEMENT, DISTANCE, ORIENTATION, or STRAIGHT
        selfieThreshold = 0.0,                 // Min backend selfie confidence to progress (0.0 disables)
        captureMethod = CaptureMethod.CAMERA   // CAMERA, UPLOAD, or BOTH
    )
)
```

- `detectionMode`: `ENHANCED_WITH_FALLBACK` (enhanced liveness first, falling back to classic) or `CLASSIC_ONLY` (platform-native detection).
- `timeoutMs`: if no suitable image is captured within this time, the flow lets the user take a photo manually. `null` disables the timeout.
- `livenessMode`: which liveness challenge to run (`MOUTH_MOVEMENT`, `DISTANCE`, `ORIENTATION`, `STRAIGHT`).
- `selfieThreshold`: minimum confidence (0.0–1.0) required on the selfie result returned by the backend; below it, the capture stage retries. `0.0` disables the gate.
- `captureMethod`: `CAMERA` (live capture), `UPLOAD` (gallery only), or `BOTH`.

## Reading the Result

When the flow completes with `FlowResult.Success`, `result.job` is an [`APIJobResponse`] containing the full verification result:

```kotlin
is FlowResult.Success -> {
    val job = result.job
    println("Job id: ${job.id}")
    println("Status: ${job.status}")
    println("Success: ${job.result.success}")
    println("Success with suggestion: ${job.result.successWithSuggestion}")
    println("Face match confidence: ${job.result.confidences?.faceMatch}")
    println("ID confidence: ${job.result.confidences?.id}")
    println("ID quality confidence: ${job.result.confidences?.idQuality}")
    // job.result also exposes parsed fields: firstName, lastName, birthDate,
    // expireDate, idAddress, crosscheck, aamva, etc.
}
```

Key fields of `APIJobResponse`:

- `id` — the verification job identifier.
- `token` — the session token for this job.
- `status` — current job status.
- `result` ([`JobResult`]) — `success`, `successWithSuggestion`, parsed ID fields, and `confidences`:
  - `confidences` ([`Confidence`]) — `faceMatch`, `idMatch`, `idQuality`, `selfie`, `idExpired`, and more.
- `signals` — list of insights/warnings from the backend.

## Error Handling

The flow can complete with three result types (see [Quick Start](#quick-start)):

- `FlowResult.Success(job)` — verification completed.
- `FlowResult.Cancelled(reason)` — the user (or an interceptor) cancelled the flow; `reason` is optional.
- `FlowResult.Error(error, message)` — the flow failed. `error` is the underlying `Throwable`; `message` is a human-readable description.

Additional error behavior:

- **Invalid API key:** the SDK validates the API key at flow startup and surfaces an [`ApiKeyValidationException`] via the `onError` callback; the initialization stage shows a retry screen.
- **Initialization failures:** if SDK initialization or job creation fails, the flow shows a retry screen with the error message before completing with an error result.
- **Use `onError` for logging:** the `onError` callback fires before the flow completes with `FlowResult.Error`, so you can log or track the failure there and still handle the terminal state in `onFlowComplete`.

```kotlin
val flowBuilder = VouchedSDK
    .createFlow(appConfig)
    .onError { error ->
        Log.e("Vouched", "Flow failed", error)
    }
    .onFlowComplete { result ->
        when (result) {
            is FlowResult.Success -> { /* ... */ }
            is FlowResult.Cancelled -> { /* ... */ }
            is FlowResult.Error -> { /* ... */ }
        }
    }
```

## Sample App

This repository includes a working sample app under `sample/android/` that demonstrates the full integration.

### Setup the Sample App

1. Clone this repository
2. Open `sample/android/` in Android Studio
3. Create a `local.properties` file in the repository root (see `local.properties.example`) and add your Vouched API key:

```properties
VOUCHED_API_KEY=your_actual_api_key
```

4. Sync and run the project on a physical device (camera required)

### Run

Build and run on a physical Android device. Camera access is required for ID and face capture; some features may not work properly on emulators.

## Versioning

This SDK follows Semantic Versioning. Each tagged release on this repository corresponds to one published AAR, hosted on Maven Central.

## Support

- Documentation: https://docs.vouched.id
- Contact: support@vouched.id

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.

## Migrating from v1

If you were using the v1 SDK (`id.vouched.android:vouched-sdk`), see [MIGRATION.md](MIGRATION.md) for a full breakdown of what changed and how to upgrade.

## Legacy SDK

Looking for the v1.x SDK? See the [vouched-android repository](https://github.com/vouched/vouched-android).
