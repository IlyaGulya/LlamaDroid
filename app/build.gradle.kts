plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlin.kapt)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.anvil)
}

android {
    namespace = "me.gulya.llamadroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.gulya.llamadroid"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                this.abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    ndkVersion = "26.3.11579264"
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")

        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.mvikotlin)
    implementation(libs.mvikotlin.main)
    implementation(libs.mvikotlin.logging)
    implementation(libs.mvikotlin.extensions.coroutines)

    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)

    implementation(libs.essenty.lifecycle)
    implementation(libs.essenty.lifecycle.coroutines)
    implementation(libs.essenty.stateKeeper)
    implementation(libs.essenty.backHandler)

    api(libs.circuit.codegen.annotations)
    ksp(libs.circuit.codegen)
    implementation(libs.circuit.foundation)
    implementation(libs.circuit.runtime.ui)
    implementation(libs.circuit.retained)

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    implementation(libs.napier)
    implementation(libs.multiplatformSettings)

}