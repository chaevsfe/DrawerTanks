import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    gradleApi()
    implementation(group = "net.darkhax.curseforgegradle", name = "CurseForgeGradle", version = "1.1.26")
    implementation(group = "com.modrinth.minotaur", name = "Minotaur", version = "2.8.+")
}

val asmVersion = "9.10.1"

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm") {
            useVersion(asmVersion)
            because("MC 26.2 is Java 25 bytecode; an older ASM here silently breaks Loom's jar merge")
        }
    }
}