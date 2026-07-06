plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.1")
}

sourceSets {
    named("main") {
        java.setSrcDirs(
            listOf(
                "${rootProject.projectDir}/src/main/java/chat/delta/rpc",
                "${rootProject.projectDir}/src/main/java/chat/delta/util",
                "${rootProject.projectDir}/polli-engine/src/jvmMain/java/chat/delta/rpc",
            ),
        )
    }
}
