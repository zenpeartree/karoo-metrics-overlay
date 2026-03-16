plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zenpeartree.karoometricsoverlay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zenpeartree.karoometricsoverlay"
        minSdk = 23
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = providers.gradleProperty("KEYSTORE_PASSWORD")
                .orElse(providers.environmentVariable("KEYSTORE_PASSWORD"))
                .get()
            keyAlias = "karoo-metrics"
            keyPassword = storePassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    implementation("io.hammerhead:karoo-ext:1.1.8")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}
