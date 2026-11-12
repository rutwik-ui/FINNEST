plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.finnest.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.finnest.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // This is the same public-client key used by the website; RLS remains the authorization boundary.
        buildConfigField("String", "GUIDANCE_URL", "\"https://wyjjzynjoorjmpffndzu.supabase.co\"")
        buildConfigField("String", "GUIDANCE_PUBLISHABLE_KEY", "\"sb_publishable_Nzb2UydbWvi6wJTtdt0_oA_pRuAWfWf\"")
    }
    buildFeatures { viewBinding = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
