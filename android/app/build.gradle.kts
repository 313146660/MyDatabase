plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.mydb"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.mydb"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "2.10"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.api.ApkVariantOutput).outputFileName =
                "MyDB-$versionName.apk"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
}

configurations.all {
    resolutionStrategy {
        // Kotlin 2.0 已把 jdk7/jdk8 合并进 stdlib；这里只是为了在本机离线缓存里对齐可用版本
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.20")
    }
}
