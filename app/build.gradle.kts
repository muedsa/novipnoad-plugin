import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists() && keystorePropertiesFile.canRead()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.muedsa.tvbox.demoplugin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.muedsa.tvbox.demoplugin"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.containsKey("muedsa.signingConfig.storeFile")) {
                storeFile = file(keystoreProperties["muedsa.signingConfig.storeFile"] as String)
                storePassword = keystoreProperties["muedsa.signingConfig.storePassword"] as String
                keyAlias = keystoreProperties["muedsa.signingConfig.keyAlias"] as String
                keyPassword = keystoreProperties["muedsa.signingConfig.keyPassword"] as String
            } else {
                val debugSigningConfig = signingConfigs.getByName("debug")
                storeFile = debugSigningConfig.storeFile
                storePassword = debugSigningConfig.storePassword
                keyAlias = debugSigningConfig.keyAlias
                keyPassword = debugSigningConfig.keyPassword
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
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}
dependencies {
    //implementation(libs.androidx.core.ktx)
    implementation(project(":api"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}