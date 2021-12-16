# Vouched

[![GitHub release](https://img.shields.io/github/release/vouched/vouched-android.svg?maxAge=60)](https://github.com/vouched/vouched-android/releases)
[![License](https://img.shields.io/github/license/vouched/vouched-android)](https://github.com/vouched/vouched-android/blob/master/LICENSE)

## Run Example

Clone this repo and change directory to _example_

```shell
git clone https://github.com/vouched/vouched-android

cd vouched-android/example
```

Then, follow steps listed on the [example README](https://github.com/vouched/vouched-android/blob/master/example/README.md)

## Prerequisites

- An account with Vouched
- Your Vouched Public Key
- Mobile Assets (available on the dashboard)

## Install

#### Add the package to your existing project

```shell
implementation 'id.vouched.android:vouched-sdk:VOUCHED_VERSION'
```

#### (Optional) Add barcode scanning
In order to use [BarcodeDetect](#barcodedetect), you must add [ML Kit Barcode Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/android).  
Note: you can choose between the bundled and unbundled model.
```shell

// Use this dependency to bundle the model with your app
implementation 'com.google.mlkit:barcode-scanning:16.2.0'  

// Use this dependency to use the dynamically downloaded model in Google Play Services
implementation 'com.google.android.gms:play-services-mlkit-barcode-scanning:16.2.0'
```

#### (Optional) Add face detection
In order to use [FaceDetect](#facedetect), you must add [ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection/android).  
Note: you can choose between the bundled and unbundled model. The unbundled model will provide a smaller app footprint, but will require connectivity to download the model when verification is run.
```shell

// Use this dependency to bundle the model with your app
implementation 'com.google.mlkit:face-detection:16.1.2'  

// Use this dependency to use the dynamically downloaded model in Google Play Services
implementation 'com.google.android.gms:play-services-mlkit-face-detection:16.2.0'
```

## Getting Started

This section will provide a _step-by-step_ path to understand the Vouched SDK through the Example.

0. [Get familiar with Vouched](https://docs.vouched.id/#section/Overview)

1. [Run the Example](#run-example)
   - Go through the verification process but stop after each step and take a look at the logs. Particularly understand the [Job](https://docs.vouched.id/#tag/job-model) data from each step.
   ```java
   System.out.println(job.toJson());
   ```
   - Once completed, take a look at the [Job details on your Dashboard](https://docs.vouched.id/#section/Dashboard/Jobs)
2. Modify the Listeners

   - Locate the [JobResponseListener](#jobresponselistener) in each Activity and make modifications.

     - Comment out the [RetryableErrors](#retryableerror)
       `List<RetryableError> retryableErrors = ...`
     - Add custom logic to display data or control the navigation

   - Locate the [CardDetectResultListener](#carddetectresultlistener) and [FaceDetectResultListener](#facedetectresultlistener) and add logging

3. Tweak CameraX settings  
   Better images lead to better results from Vouched AI
4. You are ready to integrate Vouched SDK into your app

## UX Considerations

Your users will have a higher success rate if they understand the tasks they are to perform. Consider providing guidance in your application to help them. This example app uses a simple overlay to provide guidance as how to optimally place an ID document, but you might consider informing the user of the following:
- Lay ID on a flat surface
- Minimize glare
- Try not to tilt the phone relative to the ID

## Reference

### VouchedCameraHelper

This class is introduced to make it easier for developers to integrate VouchedSDK and provide the optimal photography. The helper takes care of configuring the capture session, input, and output. Helper has following modes: 'ID' | 'FACE' | 'BARCODE'

##### Initialize

```java
VouchedCameraHelper cameraHelper = new VouchedCameraHelper(this, this, ContextCompat.getMainExecutor(this), previewView, VouchedCameraHelper.Mode.ID, new VouchedCameraHelperOptions.Builder()
                .withCardDetectOptions(new CardDetectOptions.Builder()
                        .withEnableDistanceCheck(false)
                        .build())
                .withCardDetectResultListener(this)
                .withBarcodeDetectResultListener(this)
                .build());
```

| Parameter Type                      | Nullable |
| --------------                      | :------: |
| android.content.Context             |  false   |
| androidx.lifecycle.LifecycleOwner   |  false   |
| java.util.concurrent.Executor       |  false   |
| androidx.camera.view.PreviewView    |  false   |
| [VouchedCameraHelper.Mode](#vouchedcamerahelpermode)          |  false   |
| [VouchedCameraHelper.Options](#vouchedcamerahelperoptions)          |  false   |


### CameraX

We recommend using [CameraX](https://developer.android.com/training/camerax) with the Vouched SDK. The references will all use CameraX.

### VouchedSession

This class handles a user's Vouched session. It takes care of the API calls. Use one instance for the duration of a user's verification session.

##### Initialize

```java
VouchedSession session = new VouchedSession("PUBLIC_KEY");
```

| Parameter Type | Nullable |
| -------------- | :------: |
| String         |  false   |

##### Initializing with token
```java
VouchedSession session = new VouchedSession("PUBLIC_KEY", new VouchedSessionParameters.Builder().withToken("TOKEN").build());
```
        
##### POST Front Id image

```java
session.postFrontId(this, cardDetectResult, new Params.Builder(), this);
```

| Parameter Type                              | Nullable |
| ------------------------------------------- | :------: |
| android.content.Context                     |  false   |
| [CardDetectResult](#carddetectresult)       |  false   |
| [ParamsBuilder](#paramsbuilder)             |   true   |
| [JobResponseListener](#jobresponselistener) |  false   |

##### POST Selfie image

```java
session.postFace(this, faceDetectResult, new Params.Builder(), this);
```

| Parameter Type                              | Nullable |
| ------------------------------------------- | :------: |
| android.content.Context                     |  false   |
| [FaceDetectResult](#facedetectresult)       |  false   |
| [ParamsBuilder](#paramsbuilder)             |   true   |
| [JobResponseListener](#jobresponselistener) |  false   |

##### POST confirm verification

```javascript
session.confirm(this, null, this);
```

| Parameter Type                              | Nullable |
| ------------------------------------------- | :------: |
| android.content.Context                     |  false   |
| [ParamsBuilder](#paramsbuilder)             |   true   |
| [JobResponseListener](#jobresponselistener) |  false   |

### CardDetect

This class handles detecting an ID (cards and passports) and performing necessary steps to ensure image is POSTABLE.

##### Initialize

```java
CardDetect cardDetect = new CardDetect(getAssets(), new CardDetectOptions.Builder().withEnableDistanceCheck(true).build(), this);
```

| Parameter Type                                                 | Nullable |
| -------------------------------------------------------------- | :------: |
| android.content.res.AssetManager                               |  false   |
| [CardDetectOptions](#carddetectoptions)                        |  false   |
| [CardDetect.OnDetectResultListener](#carddetectresultlistener) |  false   |

##### Process Image

```java
cardDetect.processImageProxy(imageProxy, handler);
```

| Parameter Type                  | Nullable |
| ------------------------------- | :------: |
| androidx.camera.core.ImageProxy |  false   |
| android.os.Handler              |  false   |

### BarcodeDetect

This class handles detecting the encoded barcode data. Only applicable for ID and DL cards.

##### Initialize

```java
BarcodeDetect barcodeDetect = new BarcodeDetect(this);
```

| Parameter Type                                                 | Nullable |
| -------------------------------------------------------------- | :------: |
| [BarcodeDetect.OnBarcodeResultListener](#barcodedetectresultlistener) |  false   |

##### Process Image

```java
cardDetect.findBarcode(imageProxy);
```

| Parameter Type                  | Nullable |
| ------------------------------- | :------: |
| androidx.camera.core.ImageProxy |  false   |

### FaceDetect

This class handles detecting a face and performing necessary steps to ensure image is POSTABLE.

##### Initialize

```java
FaceDetect faceDetect = new FaceDetect(this, new FaceDetectOptions.Builder().withLivenessMode(LivenessMode.DISTANCE).build(), this);
```

| Parameter Type                                                 | Nullable |
| -------------------------------------------------------------- | :------: |
| android.content.Context                                        |  false   |
| [FaceDetectOptions](#facedetectoptions)                        |  false   |
| [FaceDetect.OnDetectResultListener](#facedetectresultlistener) |  false   |

##### Process Image

```java
faceDetect.processImageProxy(imageProxy, graphicOverlay);
```

| Parameter Type                  | Nullable |
| ------------------------------- | :------: |
| androidx.camera.core.ImageProxy |  false   |
| GraphicOverlay                  |   true   |

### Types

##### CardDetectResult

The output from [Card Detection](#carddetect) and used to submit an ID.

```java
class CardDetectResult {
    public Step getStep() { ... }

    public Instruction getInstruction() { ... }

    public String getImage() { ... }

    public String getDistanceImage() { ... }
}
```

##### VouchedCameraHelperMode

An enum to provide mode for [VouchedCameraHelper](#vouchedcamerahelper)

```java
enum Mode {
        ID,
        BARCODE,
        FACE
    }
```

##### VouchedCameraHelperOptions

List of options to alter image processing for [VouchedCameraHelper](#vouchedcamerahelper)

```java
    VouchedCameraHelperOptions cameraOptions = new VouchedCameraHelperOptions.Builder()
                .withFaceDetectOptions(new FaceDetectOptions.Builder()
                        .withLivenessMode(LivenessMode.MOUTH_MOVEMENT)
                        .build())
                .withFaceDetectResultListener(this)
                .build());

```

##### BarcodeDetectResult

The output from [Barcode Detection](#barcodedetect) and used to submit the encoded Barcode data.

```java
class BarcodeResult {
    public String getValue() { ... }

    public String getImage() { ... }
}
```

##### FaceDetectResult

The output from [Face Detection](#facedetect) and used to submit a Selfie.

```java
class FaceDetectResult {
    public Step getStep() { ... }

    public Instruction getInstruction() { ... }

    public String getImage() { ... }

    public String getUserDistanceImage() { ... }
}
```

##### ParamsBuilder

The builder for the parameters that are used to submit a Job.

```java
class Builder {
    public Builder withFirstName(String firstName) { ... }

    public Builder withLastName(String lastName) { ... }

    public Builder withIdPhoto(String idPhoto) { ... }

    public Builder withUserPhoto(String userPhoto) { ... }

    public Builder withUserDistancePhoto(String userDistancePhoto) { ... }

    public Builder withIdDistancePhoto(String idDistancePhoto) { ... }

    public Params build() { ... }
}
```

##### JobResponseListener

The listener to retrieve the [Job](https://docs.vouched.id/#tag/job-model) data from the submission.

```java
public interface OnJobResponseListener {
    void onJobResponse(JobResponse response);
}
```

Follow the below template

```java
@Override
public void onJobResponse(JobResponse response) {
    if (response.getError() != null) {
        // handle app/network/system errors
    }

    // debug Job data
    System.out.println(response.getJob().toJson());

    // implement business and navigation logic based on Job data
}
```

##### CardDetectOptions

The options for [Card Detection](#carddetect).

```java
class Builder {
    public Builder withEnableDistanceCheck(boolean enableDistanceCheck) { ... }

    public CardDetectOptions build() { ... }
}
```

##### CardDetectResultListener

The listener to retrieve [CardDetectResult](#carddetectresult).

```java
interface OnDetectResultListener {
    void onCardDetectResult(CardDetectResult cardDetectResult);
}
```

##### BarcodeDetectResultListener

The listener to retrieve [BarcodeDetectResult](#barcodedetectresult).

```java
interface OnBarcodeResultListener {
    void onBarcodeResult(BarcodeResult barcodeResult);
}
```

##### FaceDetectOptions

The options for [Face Detection](#facedetect).

```java
class Builder {
    public Builder withLivenessMode(LivenessMode livenessMode) { ... }

    public FaceDetectOptions build() { ... }
}
```

##### FaceDetectResultListener

The listener to retrieve [FaceDetectResult](#facedetectresult).

```java
interface OnDetectResultListener {
    void onFaceDetectResult(FaceDetectResult faceDetectResult);
}
```

##### RetryableError

An enum to provide an optional baseline of Verification Error(s) for a given Job.

```java
enum RetryableError {
    InvalidIdPhotoError,
    InvalidUserPhotoError,
    BlurryIdPhotoError,
    GlareIdPhotoError
}
```
