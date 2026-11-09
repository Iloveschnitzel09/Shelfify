import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

val versionPropsFile = file("version.properties")

if (!versionPropsFile.exists()) {
    versionPropsFile.writeText("VERSION_CODE=1\nVERSION_NAME=1.0")
}

val versionProps = Properties()
versionProps.load(versionPropsFile.inputStream())

var nextVersionCode = versionProps.getProperty("VERSION_CODE").toInt()
var nextVersionName: String = versionProps.getProperty("VERSION_NAME")

nextVersionCode++
val versionParts = nextVersionName.split(".").toMutableList()
versionParts[versionParts.size - 1] = (versionParts.last().toInt() + 1).toString()
nextVersionName = versionParts.joinToString(".")

versionProps.setProperty("VERSION_CODE", nextVersionCode.toString())
versionProps.setProperty("VERSION_NAME", nextVersionName)
versionProps.store(versionPropsFile.outputStream(), null)

// Eigene Namen für die Werte, damit sie im defaultConfig-Block nicht mit
// den gleichnamigen Properties (versionCode/versionName) verwechselt werden
val appVersionCode: Int = nextVersionCode
val appVersionName: String = nextVersionName

// 3. DER ANDROID BLOCK
configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "de.schnitzel.shelfify"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.schnitzel.shelfify"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugSign") {
            storeFile = file(project.property("MY_DEBUG_STORE_FILE").toString())
            storePassword = project.property("MY_DEBUG_STORE_PASSWORD").toString()
            keyAlias = project.property("MY_DEBUG_KEY_ALIAS").toString()
            keyPassword = project.property("MY_DEBUG_KEY_PASSWORD").toString()
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debugSign")
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("debugSign")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.material)
    implementation(libs.identity.jvm)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}