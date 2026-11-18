plugins { id("com.android.application") }
android {
    namespace = "app.moviebox.mobile"
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    defaultConfig {
        applicationId = "app.moviebox.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.6.1"
        buildConfigField("String", "ENGINE_VERSION", "\"0.1.30\"")
        buildConfigField("String", "ENGINE_COMMIT", "\"1fe8874d9d13b11ead730364dc58ac6576ba1fcf\"")
        buildConfigField("String", "UPDATE_REPOSITORY", "\"${providers.gradleProperty("updateRepository").getOrElse("SayemXx/cubox-chill")}\"")
        testInstrumentationRunner = providers.gradleProperty("testRunner").getOrElse("app.moviebox.mobile.SmokeInstrumentation")
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { jniLibs { useLegacyPackaging = false } }
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    implementation("androidx.media3:media3-database:1.9.0")
    implementation("androidx.core:core:1.17.0")
    testImplementation("junit:junit:4.13.2")
}
