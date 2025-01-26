
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
    exclude("META-INF/versions/9/module-info.class")
    exclude("META-INF/versions/9/module-info.java")
}

tasks.shadowJar {
    exclude("META-INF/versions/9/module-info.class")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

for (jarTask in listOf(tasks.jar, tasks.shadowJar)) {
    jarTask.configure {
        manifest {
            attributes("Multi-Release" to true)
        }
    }
}
