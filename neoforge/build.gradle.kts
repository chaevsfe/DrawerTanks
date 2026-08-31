import com.texelsaurus.Properties
import com.texelsaurus.Versions
import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
    id("modloader-conv")
    id("net.neoforged.moddev") version ("2.0.143")
    id("com.modrinth.minotaur")
}

neoForge {
    version = Versions.neoForge
    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
            programArgument("--nogui")
        }
    }

    mods {
        register(Properties.modid) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.named<JavaExec>("runServer") {
    standardInput = System.`in`
}

dependencies {
    compileOnly(Versions.sdModrinthNeoforge)
    compileOnly("maven.modrinth:jade:${Versions.jadeNeoforge}")
    "localOnlyRuntime"(Versions.sdModrinthNeoforge)
}

val lastChangelog = File(rootDir, "CHANGELOG.last.md").takeIf { it.exists() }?.readText() ?: ""

tasks.create<TaskPublishCurseForge>("publishCurseForge") {
    dependsOn(tasks.jar)

    disableVersionDetection()
    apiToken = System.getenv("CURSEFORGE_API_KEY") ?: "debug_key"

    val mainFile = upload(Properties.curseProjectId, tasks.jar.get().archiveFile)
    mainFile.displayName = "${Properties.name}-${Versions.minecraft}-neoforge-$version"
    mainFile.changelogType = "markdown"
    mainFile.changelog = lastChangelog
    mainFile.releaseType = Properties.distRelease
    Properties.distGameVersions.split(',').forEach { v -> mainFile.addGameVersion(v) }
    mainFile.addModLoader("NeoForge")
    mainFile.addEnvironment("Client", "Server")
    mainFile.addOptional("jade")
}

modrinth {
    token.set(System.getenv("MODRINTH_API_KEY") ?: "debug_key")
    projectId.set(Properties.modrinthProjectId)
    changelog.set(lastChangelog)
    versionName.set("${Properties.name}-${Versions.minecraft}-neoforge-$version")
    versionNumber.set("${Versions.minecraft}-${Versions.mod}+neoforge")
    versionType.set(Properties.distRelease)
    gameVersions.set(Properties.distGameVersions.split(','))
    uploadFile.set(tasks.jar.get())
    loaders.add("neoforge")

    dependencies {
        required.project(Properties.storageDrawersModrinthId)
        optional.project("jade")
    }
}
tasks.modrinth.get().dependsOn(tasks.jar)
