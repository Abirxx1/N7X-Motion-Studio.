plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.n7x.motionstudio"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.n7x.motionstudio"
        minSdk = 28
        targetSdk = 34
        versionCode = 17
        versionName = "0.8.2"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-effect:1.4.1")
    implementation("androidx.media3:media3-transformer:1.4.1")
    implementation("com.google.guava:guava:33.2.1-android")
}

// The filter and transition previews use these two pictures. Fail the build if they are missing.
tasks.register("verifySamples") {
    val files = listOf(
        file("src/main/res/drawable-nodpi/sample_a.jpg"),
        file("src/main/res/drawable-nodpi/sample_b.jpg")
    )
    doLast {
        files.forEach { f ->
            require(f.exists() && f.length() > 5000L) { "Sample picture is missing or empty: ${f.name}" }
        }
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn("verifySamples") }
