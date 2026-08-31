import com.texelsaurus.Properties
import com.texelsaurus.Versions

plugins {
    id("modloader-conv")
    id("net.neoforged.moddev") version ("2.0.143")
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
