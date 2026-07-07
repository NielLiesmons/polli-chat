plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    namespace = "com.polli.state.cryptree"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":polli-core"))
            implementation(project(":polli-domain"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
        val jvmTest by getting {
            dependsOn(commonTest.get())
        }
    }
}
