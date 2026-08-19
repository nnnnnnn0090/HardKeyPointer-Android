plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    val releaseStoreFile = providers.gradleProperty("RELEASE_STORE_FILE")
    val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (releaseStoreFile.isPresent && releaseStorePassword.isPresent &&
                releaseKeyAlias.isPresent && releaseKeyPassword.isPresent
            ) {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    namespace = "com.nnnnnnn0090.hardkeypointer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nnnnnnn0090.hardkeypointer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseStoreFile.isPresent && releaseStorePassword.isPresent &&
                releaseKeyAlias.isPresent && releaseKeyPassword.isPresent
            ) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
