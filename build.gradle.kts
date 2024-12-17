
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.processResources {
    inputs.property("version", project.version.toString())
    filesMatching("META-INF/rfb-plugin/*") {
        expand("version" to project.version.toString())
    }
}

// Shadow source jars too
val shadowSources = configurations.getByName("shadowSources")
tasks.sourcesJar {
    from(shadowSources.map { zipTree(it) })
}
