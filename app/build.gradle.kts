import com.android.build.gradle.ProguardFiles.getDefaultProguardFile
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

// Agora/LLM credentials live in local.properties (git-ignored) and surface as BuildConfig fields.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProperty(key: String, fallback: String = ""): String =
    localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

android {
    namespace = "com.example.pettalk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.pettalk"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AGORA_APP_ID", "\"${localProperty("AGORA_APP_ID")}\"")
        buildConfigField("String", "AGORA_RTC_TOKEN", "\"${localProperty("AGORA_RTC_TOKEN")}\"")
        buildConfigField("String", "AGORA_RTM_TOKEN", "\"${localProperty("AGORA_RTM_TOKEN")}\"")
        buildConfigField("String", "AGORA_CUSTOMER_KEY", "\"${localProperty("AGORA_CUSTOMER_KEY")}\"")
        buildConfigField("String", "AGORA_CUSTOMER_SECRET", "\"${localProperty("AGORA_CUSTOMER_SECRET")}\"")
        buildConfigField(
            "String",
            "CONVOAI_PRESET",
            "\"${localProperty("CONVOAI_PRESET", "deepgram_nova_3,openai_gpt_5_mini,minimax_speech_2_6_turbo")}\""
        )
        buildConfigField("String", "CONVOAI_LLM_URL", "\"${localProperty("CONVOAI_LLM_URL")}\"")
        buildConfigField("String", "CONVOAI_LLM_MODEL", "\"${localProperty("CONVOAI_LLM_MODEL")}\"")
        buildConfigField("String", "CONVOAI_LLM_API_KEY", "\"${localProperty("CONVOAI_LLM_API_KEY")}\"")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)

    // Agora RTC + RTM: audio transport and Conversation AI messages
    implementation(libs.agora.rtc)
    implementation(libs.agora.rtm)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
