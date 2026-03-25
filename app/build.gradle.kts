plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystoreFile = file("../release.keystore")
val releaseStorePassword = providers.gradleProperty("KEYSTORE_PASSWORD")
    .orElse(providers.environmentVariable("KEYSTORE_PASSWORD"))
    .orNull
val hasReleaseSigning = releaseKeystoreFile.exists() && !releaseStorePassword.isNullOrBlank()

android {
    namespace = "com.zenpeartree.karoometricsoverlay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zenpeartree.karoometricsoverlay"
        minSdk = 23
        targetSdk = 34
        versionCode = 8
        versionName = "1.1.6"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = "karoo-metrics"
                keyPassword = releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

if (!hasReleaseSigning) {
    logger.lifecycle("Release signing disabled: missing ../release.keystore or KEYSTORE_PASSWORD.")
}
