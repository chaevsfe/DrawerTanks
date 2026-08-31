import com.texelsaurus.Properties
import com.texelsaurus.Versions
import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
    id("modloader-conv")
    id("net.fabricmc.fabric-loom") version "1.18.0-alpha.9"
    id("com.modrinth.minotaur")
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    implementation("net.fabricmc:fabric-loader:${Versions.fabricLoader}")
    implementation("net.fabricmc.fabric-api:fabric-api:${Versions.fabric}")

    compileOnly(Versions.sdModrinthFabric)
    compileOnly("maven.modrinth:jade:${Versions.jadeFabric}")
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

val lastChangelog = File(rootDir, "CHANGELOG.last.md").takeIf { it.exists() }?.readText() ?: ""

tasks.create<TaskPublishCurseForge>("publishCurseForge") {
    dependsOn(tasks.jar)

    disableVersionDetection()
    apiToken = System.getenv("CURSEFORGE_API_KEY") ?: "debug_key"

    val mainFile = upload(Properties.curseProjectId, tasks.jar.get().archiveFile)
    mainFile.displayName = "${Properties.name}-${Versions.minecraft}-fabric-$version"
    mainFile.changelogType = "markdown"
    mainFile.changelog = lastChangelog
    mainFile.releaseType = Properties.distRelease
    Properties.distGameVersions.split(',').forEach { v -> mainFile.addGameVersion(v) }
    mainFile.addModLoader("Fabric")
    mainFile.addEnvironment("Client", "Server")
    mainFile.addRequirement("fabric-api")
    mainFile.addOptional("forge-config-api-port")
    mainFile.addOptional("jade")
}

modrinth {
    token.set(System.getenv("MODRINTH_API_KEY") ?: "debug_key")
    projectId.set(Properties.modrinthProjectId)
    changelog.set(lastChangelog)
    versionName.set("${Properties.name}-${Versions.minecraft}-fabric-$version")
    versionNumber.set("${Versions.minecraft}-${Versions.mod}")
    versionType.set(Properties.distRelease)
    gameVersions.set(Properties.distGameVersions.split(','))
    uploadFile.set(tasks.jar.get())
    loaders.add("fabric")

    dependencies {
        required.project(Properties.storageDrawersModrinthId)
        required.project("fabric-api")
        optional.project("forge-config-api-port")
        optional.project("jade")
    }
}
tasks.modrinth.get().dependsOn(tasks.jar)
