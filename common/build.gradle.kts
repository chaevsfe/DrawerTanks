import com.texelsaurus.Versions

plugins {
    id("java-conv")
    id("net.fabricmc.fabric-loom") version "1.18.0-alpha.9"
}

loom {
    accessWidenerPath = file("src/main/resources/drawertanks.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    compileOnly(Versions.sdModrinthFabric)
}

configurations {
    register("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    register("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
