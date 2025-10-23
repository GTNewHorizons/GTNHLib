package com.gtnewhorizon.gtnhlib.client.renderer.shader;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

/**
 * Automatically detects for updates in the specified shader files to instantly update the shader. <strong>Make sure to
 * remove this when you're done testing. THIS WILL CRASH OUTSIDE OF DEV ENV.</strong> <br>
 * <br>
 * Note: Any Shader that gets recompiled needs to have their uniforms re-fetched and attributes rebound. To do this, use
 * the IShaderReloadRunnable.
 */
@SuppressWarnings("unused")
public class AutoShaderUpdater {

    private static AutoShaderUpdater instance;

    private WatchService watchService;
    private final List<ListeningShader> listeningShaders = new ArrayList<>();
    private final Set<Path> watchedPaths = new HashSet<>();

    public static AutoShaderUpdater getInstance() {
        if (instance == null) {
            instance = new AutoShaderUpdater();
        }
        return instance;
    }

    private AutoShaderUpdater() {
        if (Launch.blackboard == null || !((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            throw new UnsupportedOperationException(
                    "Cannot use AutoShaderUpdate in an obfuscated environment! This is an oversight by the developer!");
        }
    }

    public void registerShaderReload(ShaderProgram program, ResourceLocation vertexShader,
            ResourceLocation fragmentShader) {
        registerShaderReload(program, vertexShader, fragmentShader, null);
    }

    public void registerShaderReload(ShaderProgram program, ResourceLocation vertexShader,
            ResourceLocation fragmentShader, IShaderReloadRunnable runnable) {
        if (!vertexShader.getResourceDomain().equals(fragmentShader.getResourceDomain())) {
            throw new IllegalArgumentException("The directory of vertex and fragment shader must match!");
        }
        registerShaderReload(
                program,
                vertexShader.getResourceDomain(),
                vertexShader.getResourcePath(),
                fragmentShader.getResourcePath(),
                runnable);
    }

    public void registerShaderReload(ShaderProgram program, String domain, String vertShaderFilename,
            String fragShaderFilename) {
        this.registerShaderReload(program, domain, vertShaderFilename, fragShaderFilename, null);
    }

    public void registerShaderReload(ShaderProgram program, String domain, String vertShaderFilename,
            String fragShaderFilename, IShaderReloadRunnable runnable) {
        try {
            if (watchService == null) {
                watchService = FileSystems.getDefault().newWatchService();
                startWatchServiceListener();
            }
            Path vertexPath = getShaderPath(domain, vertShaderFilename);
            Path fragmentPath = getShaderPath(domain, fragShaderFilename);
            Path parent = vertexPath.getParent();
            if (!Files.exists(vertexPath)) {
                throw new RuntimeException("Vertex shader " + vertShaderFilename + " not found!");
            }
            if (!Files.exists(fragmentPath)) {
                throw new RuntimeException("Fragment shader " + fragShaderFilename + " not found!");
            }
            if (!watchedPaths.contains(parent)) {
                parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                System.out.println("Listening for shader changes in " + parent);
                watchedPaths.add(parent);
            }
            listeningShaders.add(
                    new ListeningShader(
                            program,
                            vertShaderFilename,
                            fragShaderFilename,
                            vertexPath,
                            fragmentPath,
                            runnable));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Path getShaderPath(String domain, String path) {
        return Paths.get(new File("../src/main/resources/assets/" + domain + "/" + path).getAbsolutePath());
    }

    private void startWatchServiceListener() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        final String file = event.context().toString();
                        for (ListeningShader shader : listeningShaders) {
                            if (shader.fragmentPath.getFileName().toString().equals(file)
                                    || shader.vertexPath.getFileName().toString().equals(file)) {
                                System.out.println("Shader " + file + " changed, recompiling...");
                                Minecraft.getMinecraft().func_152344_a(shader);
                            }
                        }
                    }
                    key.reset();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "ShaderUpdateWatcher");
        thread.setDaemon(true);
        thread.start();
    }

    private static class ListeningShader implements Runnable {

        private final ShaderProgram shader;
        private final String rawVertexPath;
        private final String rawFragmentPath;
        private final Path fragmentPath;
        private final Path vertexPath;
        private final IShaderReloadRunnable runnable;

        public ListeningShader(ShaderProgram shader, String rawVertexPath, String rawFragmentPath, Path vertexPath,
                Path fragmentPath, IShaderReloadRunnable runnable) {
            this.shader = shader;
            this.rawVertexPath = rawVertexPath;
            this.rawFragmentPath = rawFragmentPath;
            this.vertexPath = vertexPath;
            this.fragmentPath = fragmentPath;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                shader.close();
                shader.reload(vertexPath, fragmentPath);
                if (runnable != null) {
                    shader.use();
                    runnable.run(shader, vertexPath, fragmentPath);
                    ShaderProgram.clear();
                }
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("Updated Shaders " + rawVertexPath + " and " + rawFragmentPath));
            } catch (Exception e) {
                e.printStackTrace();
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                                EnumChatFormatting.RED + "Failed to update "
                                        + rawVertexPath
                                        + " or "
                                        + rawFragmentPath
                                        + ". See logs for more info."));
            }
        }
    }
}
