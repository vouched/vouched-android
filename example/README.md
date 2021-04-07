# Vouched Android Example

## Features in the Example

**1st Screen** - Name Input (Optional)  
**2st Screen** - ID Detection  
**3nd Screen** - Face Detection  
**4th Screen** - ID Verification Results

When you finish, Vouched has performed the following features to verify your identity

- ID Card and Passport Detection
- Face Detection (with liveness)
- ID Verification
- Name Verification

## Getting Started

### IDE

We suggest to use [Android Studio](https://developer.android.com/studio) to run and modify the Example.

### Environment

1. If necessary, navigate to your Vouched Dashboard and create a [Public Key](https://docs.vouched.id/#section/Dashboard/Manage-keys).
2. Create gradle.properties

```
android.useAndroidX=true
API_KEY="<PUBLIC_KEY>"
```

note: `"` is required for strings

### Add Vouched Assets

1. Navigate to your Vouched Dashboard and download the Android [Mobile Assets](https://docs.vouched.id/#section/Dashboard/Mobile-Assets).
2. Unzip and copy the `assets` directory to src/main. The result will be `src/main/assets`

### Build

Use Android Studio Gradle Plugin to build broject

### Run

Unfortunately, cameras are not supported in simulators so the best way to run the example is on a real device. Once your device is plugged in, run the Example through Android Studio
