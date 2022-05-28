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

We suggest to use [Android Studio](https://developer.android.com/studio) to run and modify the Example. Switch to the [Project View](https://developer.android.com/studio/projects#ProjectView) in order to view all the files associated with the project.

### Environment

1. If necessary, navigate to your Vouched Dashboard and create a [Public Key](https://docs.vouched.id/#section/Dashboard/Manage-keys).
2. Create gradle.properties
3. Expose the key in your gradle app module

```
API_KEY="<PUBLIC_KEY>"
```

note: `"` is required for strings

### Build

Use Android Studio Gradle Plugin to build broject

### Run

Unfortunately, cameras are not supported in simulators so the best way to run the example is on a real device. Once your device is plugged in, run the Example through Android Studio
