plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    namespace = "com.polli.domain"
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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val jvmTest by getting {
            dependsOn(commonTest.get())
        }
    }
}
