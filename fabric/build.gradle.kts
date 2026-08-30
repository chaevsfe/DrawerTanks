import com.texelsaurus.Versions

plugins {
    id("modloader-conv")
    id("net.fabricmc.fabric-loom") version "1.18.0-alpha.9"
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    implementation("net.fabricmc:fabric-loader:${Versions.fabricLoader}")
    implementation("net.fabricmc.fabric-api:fabric-api:${Versions.fabric}")

    compileOnly(Versions.sdModrinthFabric)
    compileOnly("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.2.1") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    "localOnlyRuntime"(Versions.sdModrinthFabric)
    "localOnlyRuntime"("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.2.1") {
        exclude(group = "net.fabricmc.fabric-api")
    }
}

loom {
    accessWidenerPath = file("src/main/resources/drawertanks.fabric.accesswidener")
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
