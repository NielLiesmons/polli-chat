plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    namespace = "com.polli.engine"
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
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":polli-domain"))
                implementation(project(":polli-core"))
                implementation(project(":chatmail-rpc-client"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.11.1")
            }
        }

        val engineMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("src/engineMain/kotlin")
        }

        val androidMain by getting {
            dependsOn(engineMain)
        }

        val jvmMain by getting {
            dependsOn(engineMain)
            kotlin.srcDir("src/jvmMain/kotlin")
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.10.2")
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

tasks.register<JavaExec>("dumpInbox") {
    group = "polli"
    description = "Print inbox rows from live deltachat-rpc-server (close Polli Desktop first)"
    dependsOn("jvmJar")
    classpath =
        kotlin.jvm().compilations["main"].runtimeDependencyFiles +
            files(kotlin.jvm().compilations["main"].output.classesDirs) +
            project(":chatmail-rpc-client").sourceSets["main"].output.classesDirs
    mainClass.set("com.polli.engine.rpc.DumpInboxKt")
}
