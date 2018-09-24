# Katzenpost/Mail Android

This is a Katzenpost client, based on [K-9 Mail](https://github.com/k9mail/k-9).

# Build Instructions

Building the project depends on the [Android SDK](https://developer.android.com/studio/index.html), [Android NDK](https://developer.android.com/ndk/index.html), [Go 1.7+](https://golang.org/), and [gomobile](https://github.com/golang/go/wiki/Mobile).

## Prepare Toolchain

1. Download Android SDK https://developer.android.com/sdk/download.html
2. Extract SDK to $HOME/android\_sdk
3. Download Android NDK 16.1 (or newer) https://developer.android.com/ndk/downloads/index.html
4. Extract NDK to $HOME/android\_sdk/ndk-bundle
5. Install Go 1.7 https://golang.org/doc/install
6. Add to PATH: $HOME/android\_sdk/platform\_tools and $HOME/android\_sdk/tools
7. `export ANDROID_HOME=~/android_sdk`
8. `git clone github.com/katzenpost/katzenpost-android-mail`

## Prepare Gomobile

1. Set a GOPATH environment variable
2. `go get golang.org/x/mobile/cmd/gomobile`
3. `gomobile init -ndk $HOME/android\_sdk/ndk-bundle`

## Prepare Repository and Compile Native Bindings

1. `cd katzenpost-android-mail`
2. `mkdir bindings; cd bindings`
3. `go get -v github.com/katzenpost/bindings/java`
4. `gomobile bind -v -target android github.com/katzenpost/bindings/java`

## Compile application

1. `./gradlew`
2. `./gradlew app:k9mail:assembleDebug`
3. `adb install -r app/k9mail/build/outputs/apk/debug/k9mail-debug.apk`


# Supported by

![](https://katzenpost.mixnetworks.org/_static/images/eu-flag-tiny.jpg)

This project has received funding from the European Union’s Horizon 2020
research and innovation programme under the Grant Agreement No 653497, Privacy
and Accountability in Networks via Optimized Randomized Mix-nets (Panoramix).
