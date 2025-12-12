import com.android.build.gradle.internal.tasks.DexMergingTask
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.writer.io.FileDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
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
    namespace = "com.muedsa.tvbox.novipnoad"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.muedsa.tvbox.novipnoad"
        minSdk = 24
        targetSdk = 36
        versionCode = 19
        versionName = "0.2.4"
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

    // 修改APK文件名
    applicationVariants.all {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                outputFileName = "${rootProject.name}-${versionName}-${buildType.name}.apk.tbp"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(project(":api"))
    testImplementation(project(":api"))
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}

tasks.withType<DexMergingTask> {
    doLast {
        println("taskName: $name")
        if (name.startsWith("mergeExtDex")) {
            outputDir.get().asFile.listFiles()?.forEach {
                removePackagesFromDex(
                    file = it,
                    excludedPackages = listOf("kotlin", "kotlinx", "org.intellij", "org.jetbrains"),
                )
            }
        }
    }
}
fun removePackagesFromDex(file: File, excludedPackages: List<String>) {
    println("removePackagesFromDex: ${file.absolutePath}")
    val dexFile = DexFileFactory.loadDexFile(file, null)
    val excludedClassTypes = excludedPackages.map { "L${it.replace(".", "/")}/" }
    val filteredClasses = dexFile.classes.filter { classDef ->
        val classDefType = classDef.type
        (!excludedClassTypes.any { classDefType.startsWith(it) }).apply {
            if (!this) {
                println("remove $classDefType")
            }
        }
    }
    if (filteredClasses.isNotEmpty()) {
        val dexPool = DexPool(dexFile.opcodes)
        filteredClasses.forEach {
            println("intern ${it.type}")
            dexPool.internClass(it)
        }
        dexPool.writeTo(FileDataStore(file))
    } else {
        file.delete()
    }
}
