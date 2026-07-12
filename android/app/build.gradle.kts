import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // alias(libs.plugins.kotlin.kapt) // 暂时禁用Kapt
    // alias(libs.plugins.hilt.android) // 暂时禁用Hilt
}

android {
    namespace = "com.chronie.inventorymanager"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.chronie.inventorymanager"
        minSdk = 29
        targetSdk = 36
        // 使用更兼容的日期格式化方式
        fun getBuildDate(): String {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            return String.format("%04d%02d%02d", year, month, day)
        }   

        fun getBuildTime(): String {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            return String.format("%02d%02d", hour, minute)
        }   

        val buildDate = getBuildDate()
        val defaultBuildNumber = getBuildTime()
        val buildNumber = project.findProperty("buildNumber")?.toString() ?: defaultBuildNumber

        versionCode = (System.currentTimeMillis() / 1000).toInt()
        versionName = "1.${buildDate}.${buildNumber}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures { compose = true }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xcontext-parameters",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.17" }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room数据库 (暂时禁用Kapt)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // kapt(libs.androidx.room.compiler)

    // 网络依赖
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.4.0")

    // 依赖注入 (暂时禁用Hilt)
    // implementation(libs.hilt.android)
    // kapt(libs.hilt.compiler)
    // implementation(libs.hilt.navigation.compose)

    // 协程
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // 生命周期
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coil图片加载
    implementation("io.coil-kt:coil-compose:2.7.0")

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // 可选：临时数据存储
    implementation(libs.androidx.datastore)

    // AndroidLiquidGlass毛玻璃效果库
    implementation("io.github.kyant0:backdrop:1.0.6")
    implementation("io.github.kyant0:capsule:2.1.3")
}
