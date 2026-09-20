package com.texelsaurus;

object Versions {
    const val mod = "1.3.3"
    const val java = "25"
    const val minecraft = "26.3"
    const val minecraftRange = "[26.3,26.4)"
    const val minecraftLower = "26.3"
    const val minecraftUpper = "26.4"

    const val forge = "65.1.0"
    const val forgeVersionRange = "[65,)"
    const val forgeLoaderRange = "[65,)"

    const val neoForge = "26.3.0.7-beta"
    const val neoForgeVersionRange = "[26.3.0-beta,)"
    const val neoForgeLoaderRange = "[4,)"

    const val fabric = "0.161.0+26.3"
    const val fabricLoaderMin = "0.19.5"
    const val fabricLoader = "0.19.5"

    const val storageDrawers = "19.1.8"
    // the unofficial port has no 26.3 build, so this branch builds against Texelsaur's own
    // Modrinth version ids for StorageDrawers 26.3.0.0 (fabric / neoforge files)
    const val sdModrinthFabric = "maven.modrinth:guitPqEi:KEPMtgL1"
    const val sdModrinthNeoforge = "maven.modrinth:guitPqEi:JwkKovSz"

    const val jadeFabric = "26.3.1+fabric"
    const val jadeNeoforge = "26.3.1+neoforge"
}
