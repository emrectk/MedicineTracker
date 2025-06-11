name: Build APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        distribution: 'zulu'
        java-version: '17'
        cache: 'gradle'

    - name: Make gradlew executable
      run: chmod +x ./gradlew

    - name: Build debug APK
      run: ./gradlew :app:assembleDebug

    - name: Upload APK artifact
      uses: actions/upload-artifact@v4
      with:
        name: medtracker-debug-apk
        path: app/build/outputs/apk/debug/*.apk
