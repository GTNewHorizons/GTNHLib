//version: 1707058017

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.processResources {
    inputs.property("version", project.version.toString())
    filesMatching("META-INF/rfb-plugin/*") {
        expand("version" to project.version.toString())
    }
}
