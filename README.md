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

## Install

#### Add the package to your existing project

```shell
implementation 'id.vouched.android:vouched-sdk:0.6.9'
```

#### (Optional) Add barcode scanning
In order to use [BarcodeDetect](#barcodedetect), you must add [ML Kit Barcode Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/android).  
Note: you can choose between the bundled and unbundled model. Our experience is that the bundled model provides  more accurate barcode scans. See the above ML Kit link for more information

```shell

// Use this dependency to bundle the model with your app
implementation 'com.google.mlkit:barcode-scanning:17.0.2'  

// Use this dependency to use the dynamically downloaded model in Google Play Services
implementation 'com.google.android.gms:play-services-mlkit-barcode-scanning:18.0'
```

#### (Optional) Add face detection
In order to use [FaceDetect](#facedetect), you must add [ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection/android).  
Note: you can choose between the bundled and unbundled model. The unbundled model will provide a smaller app footprint, but will require connectivity to download the model when verification is run or when the app is first installed. 

```shell

// Use this dependency to bundle the model with your app
implementation 'com.google.mlkit:face-detection:16.1.4'  

// Use this dependency to use the dynamically downloaded model in Google Play Services
implementation 'com.google.android.gms:play-services-mlkit-face-detection:17.0.0'
```

## Getting Started

This section will provide a _step-by-step_ path to understand the Vouched SDK through the Example.

0. [Get familiar with Vouched](https://docs.vouched.id/#section/Overview)

1. [Run the Example](#run-example)
   - Go through the verification process but stop after each step and take a look at the logs. Particularly understand the [Job](https://docs.vouched.id/#tag/job-model) data from each verification step.
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

## Reference

### VouchedCameraHelper

This class is introduced to make it easier for developers to integrate VouchedSDK and provide the optimal photography. The helper takes care of configuring the capture session, input, and output. Helper has following detection modes: 'ID' | 'FACE' | 'BARCODE' | 'ID_BACK'. 

##### Initialize

```java
VouchedCameraHelper cameraHelper = new VouchedCameraHelper(this, this, ContextCompat.getMainExecutor(this), previewView, VouchedCameraHelper.Mode.ID, new VouchedCameraHelperOptions.Builder()
                .withCardDetectOptions(new CardDetectOptions.Builder()
                        .withEnableDistanceCheck(false)
                        .withEnhanceInfoExtraction(false)            
                        .build())
                .withCardDetectResultListener(this)
                .withBarcodeDetectResultListener(this)
                .withCameraFlashDisabled(true)
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

### Enhanced ID Info Extraction
The camera helper can increase your verification abilities by recognizing additional sources of information based on the type of ID that your user submits.  You can enable this behavior by using  ```.withEnhanceInfoExtraction(true)``` when setting you create the camera helper.

Once enabled, the helper can help guide the ID verification modes by processing job results returned by the Vouched api service, and generating the appropriate modes that are needed to complete ID verification. 

In terms of workflow, once the front ID has been imaged and uploaded, the Vouched service identifies the type if ID that is being used, and returns as part of response a  JobResult object  that informs the SDK as to other data extraction actions that may be taken. These additional actions can include extractions of data from one or more barcodes or capturing an image of the back of the ID for firther analysis.

In the current release, some coding is necessary  - in your JobResponseListener callback, you first must verify that the job has no errors or insights (user feedback that requires more actions on the user's part before leaving a mode). If that proves to be true, pass the camera helper the results object and determine the next mode. Since you know what the next mode will be, this is a great point to dispay a dialog or provide other feedback to the user as to inform them as what to expect next.

onJobResonse changes:

```
// after verifying errors and insights, determine if the 
// ID requires other processing 
cameraHelper.updateDetectionModes(job.getResult());
// advance the mode to the next state.
VouchedCameraHelper.Mode next = cameraHelper.getNextMode();
// give the user feedback based on the next step
```

onCardDetectResult changes for back/frontside detection:

```
VouchedCameraHelper.Mode currentMode = cameraHelper.getCurrentMode();
if(currentMode.equals(VouchedCameraHelper.Mode.ID)) {
    session.postFrontId(this, cardDetectResult, new Params.Builder().withFirstName(inputFirstName).withLastName(inputLastName), this);
} else if(currentMode.equals(VouchedCameraHelper.Mode.ID_BACK)) {
    session.postBackId(this, cardDetectResult, new Params.Builder(), this);
}
```

**Note:** The DetectorActivityWithHelper class in the example app shows how enhanced extraction can be implemented. 

### CameraX

We recommend using [CameraX](https://developer.android.com/training/camerax) with the Vouched SDK. The references will all use CameraX, and in the case of the VouchedCameraHelper, the CameraX apis are a dependency of that component.

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
CardDetect cardDetect = new CardDetect(getAssets(), new CardDetectOptions.Builder().withEnableDistanceCheck(true)
                           .withEnhanceInfoExtraction(false)build(), this);
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
**Note** that CardDetectResults can arise from scanning the font or back of certain ID documents. It is currently the responsibility of the card detection callback to keep track of the mode the helper is in, and post to the correct endpoint. A future update will remove this requirement.

```java
class CardDetectResult {
    public Step getStep() { ... }

    public Instruction getInstruction() { ... }

    public String getImage() { ... }

    public String getDistanceImage() { ... }
}
```

An example of handling front and back ID images in a card detection callback:

```
VouchedCameraHelper.Mode currentMode = cameraHelper.getCurrentMode();
if(currentMode.equals(VouchedCameraHelper.Mode.ID)) {
    session.postFrontId(this, cardDetectResult, new Params.Builder().withFirstName(inputFirstName).withLastName(inputLastName), this);
} else if(currentMode.equals(VouchedCameraHelper.Mode.ID_BACK)) {
    session.postBackId(this, cardDetectResult, null, this);
}
```

##### VouchedCameraHelperMode

An enum to provide detection modes for [VouchedCameraHelper](#vouchedcamerahelper) 

```java
enum Mode {
        ID,
        BARCODE,
        ID_BACK,
        FACE,
        COMPLETED
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
    public Builder withEnhanceInfoExtraction(boolean enableEnhancedIdScan) { ... }

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
public enum LivenessMode {
    MOUTH_MOVEMENT,
    DISTANCE,
    BLINKING,
    NONE
}
```



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

