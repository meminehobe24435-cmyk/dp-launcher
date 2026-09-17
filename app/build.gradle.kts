plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dp.launcher"
    compileSdk = 35
    // Pinned to the build-tools already installed in this environment (36.0.0).
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.dp.launcher"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("en", "zh")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    buildFeatures {
        viewBinding = true
    }

    testOptions {
        unitTests {
            // Robolectric needs the merged resources to inflate the real layouts.
            isIncludeAndroidResources = true
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        warningsAsErrors = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.palette:palette-ktx:1.0.0")

    testImplementation("junit:junit:4.13.2")
    // Runs the real activity, fragments and layouts on the JVM: catches the crashes that only
    // appear once a view hierarchy is actually inflated and measured.
    testImplementation("org.robolectric:robolectric:4.14.1")
}
