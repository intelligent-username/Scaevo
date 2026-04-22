# Scaevo

Inspired by Mucius Scaevola

This is a screen usage tracker, since Samsung's default is trash. And I can't install other ones that work properly.

## Compilation Instructions

To build the application, ensure you have the correct Android SDK installed (API 34).
You can build the app from the terminal using Gradle:

```bash
# On Windows
.\gradlew.bat assembleDebug

# On macOS/Linux
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/Scaevo-debug.apk`

To install it directly onto a connected physical Android device or emulator:

```bash
# On Windows
.\gradlew.bat installDebug

# On macOS/Linux
./gradlew installDebug
```

## Release Build Instructions

To build an optimized and signed release version of Scaevo:

```bash
# On Windows
.\gradlew.bat assembleRelease

# On macOS/Linux
./gradlew assembleRelease
```

The signed APK will be located at:
`app/build/outputs/apk/release/Scaevo-release.apk`

> [!NOTE]
> We use a `keystore.properties` file in the root directory to handle signing. This file and the `release.keystore` are ignored by Git to keep your keys private.

### Setting up on a new machine

If you are cloning this repo on a new machine, you'll need to re-generate the keys:

1. Generate a keystore: `keytool -genkeypair -v -keystore release.keystore -alias scaevo -keyalg RSA -keysize 2048 -validity 10000`
2. Create `keystore.properties` in the root folder:

   ```properties
   storeFile=release.keystore
   storePassword=your_password
   keyAlias=scaevo
   keyPassword=your_password
   ```
