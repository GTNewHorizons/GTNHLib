
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

minecraft {
    //extraRunJvmArguments.add("-Dgtnhlib.dumpClass=true")
}

// Add a Java 17 sourceset for including code optimized for newer Java versions
val java17ToolchainSpec: JavaToolchainSpec.() -> Unit = {
    vendor = JvmVendorSpec.AZUL
    // Use a Java 21 compiler with a target of 17
    languageVersion = JavaLanguageVersion.of(21)
}
val main17 by sourceSets.creating {
    compileClasspath = files(compileClasspath, configurations.lwjgl3Classpath.get(), sourceSets.main.get().compileClasspath.filter({ f -> !f.toString().contains("jabel-javac") }), sourceSets.main.get().output)
    runtimeClasspath = files(runtimeClasspath, configurations.lwjgl3Classpath.get(), sourceSets.main.get().runtimeClasspath, sourceSets.main.get().output)
}
tasks.named<JavaCompile>(main17.compileJavaTaskName).configure {
    javaCompiler = javaToolchains.compilerFor(java17ToolchainSpec)
    options.release = 17
    sourceCompatibility = JavaVersion.VERSION_17.majorVersion
    targetCompatibility = JavaVersion.VERSION_17.majorVersion
}
tasks.jar.configure {
    dependsOn(main17.compileJavaTaskName)
    into("META-INF/versions/17") { from(main17.output) }
}
tasks.sourcesJar.configure {
    into("META-INF/versions/17") { from(main17.java.sourceDirectories) }
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
    into("META-INF/versions/17") { from(main17.output) }
    exclude("META-INF/versions/9/module-info.class")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // On macOS ARM64, use an ARM64-compatible JVM for tests
    val isMacOsArm64 = System.getProperty("os.name").lowercase().contains("mac") &&
        System.getProperty("os.arch") == "aarch64"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
        if (isMacOsArm64) {
            vendor = JvmVendorSpec.AZUL
        }
    }
    dependsOn(tasks.extractNatives2)
    jvmArgs("-Djava.library.path=${tasks.extractNatives2.get().destinationFolder.asFile.get().path}")
}

for (jarTask in listOf(tasks.jar, tasks.shadowJar, tasks.sourcesJar)) {
    jarTask.configure {
        manifest {
            attributes("Multi-Release" to true)
        }
    }
}
