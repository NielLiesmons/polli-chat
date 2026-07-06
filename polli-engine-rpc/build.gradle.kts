plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":polli-domain"))
    implementation(project(":polli-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("dumpInbox") {
    group = "polli"
    description = "Print inbox rows from live deltachat-rpc-server (close Polli Desktop first)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.polli.engine.rpc.DumpInboxKt")
}

sourceSets {
    named("main") {
        java.srcDirs(
            "${rootProject.projectDir}/src/main/java/chat/delta/rpc",
            "${rootProject.projectDir}/src/main/java/chat/delta/util",
            "src/main/java",
        )
        kotlin.srcDirs("src/main/kotlin")
    }
}
