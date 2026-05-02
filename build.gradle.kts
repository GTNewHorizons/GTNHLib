
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

minecraft {
    //extraRunJvmArguments.add("-Dgtnhlib.dumpClass=true")
    //extraRunJvmArguments.addAll("-Dlegacy.debugClassLoading=true", "-Dlegacy.debugClassLoadingFiner=true", "-Dlegacy.debugClassLoadingSave=true")
}

// Add a Java 17 sourceset for including code optimized for newer Java versions
val java17ToolchainSpec: JavaToolchainSpec.() -> Unit = {
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
    val projectVersion = project.version.toString()
    inputs.property("version", projectVersion)
    filesMatching("META-INF/rfb-plugin/*") {
        expand("version" to projectVersion)
    }
}

tasks.shadowJar {
    into("META-INF/versions/17") {
        from(main17.output)
    }
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

//deploader
run {

    tasks.processResources {
        from(configurations["deploader"]) {
            rename { "fplib_deploader.jar" }
        }
    }

    fun DependencyHandlerScope.depload(name: String) {
        add("compileOnlyApi", name)
        add("testImplementation", name)
        val parts = name.split(':')
        val path = "META-INF/falsepatternlib_repo/${parts[0].replace('.', '/')}/${parts[1]}/${parts[2]}/"
        val jarName = parts.subList(1, parts.size).joinToString("-") + ".jar"
        tasks.processResources {
            into(path) {
                from(configurations.compileClasspath.map { it.filter { file -> file.name.equals(jarName) } })
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    afterEvaluate {
        //defined in dependencies.gradle
        val allDeps = project.ext.get("depload_libs") as List<Pair<String, String>>
        dependencies {
            allDeps.forEach { depload(it.first) }
        }
        tasks.processResources {
            allDeps.groupBy { it.second }
                .mapValues { it.value.map { it.first } }
                .forEach { (java, deps) ->
                    filesMatching("META-INF/gtnhlib_deps${java}.json") {
                        expand("DEPS" to deps.joinToString("\", \""))
                    }
                }
        }
    }
}