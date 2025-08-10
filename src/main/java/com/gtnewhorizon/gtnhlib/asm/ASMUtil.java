package com.gtnewhorizon.gtnhlib.asm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;

import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class ASMUtil {

    private static final Logger logger = LogManager.getLogger("ASM GTNH");
    private static File outputDir = null;

    private static void emptyClassOutputFolder() {
        outputDir = new File(
                Launch.minecraftHome,
                "ASM_GTNH" + File.separatorChar + stripClassName(ASMUtil.class.getClassLoader().getClass().getName()));
        try {
            FileUtils.deleteDirectory(outputDir);
        } catch (IOException ignored) {}
        if (!outputDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            outputDir.mkdirs();
        }
    }

    /**
     * Loads the class bytes and saves the class bytes in the folder ASM_GTNH as three files, .class file, bytecode in a
     * text file, and asmified in a text file. This will not run the class transformers that might transform this class
     * during regular minecraft forge gaming
     *
     * @param clazz - the class to be saved
     */
    public static void saveClassToDisk(Class<?> clazz) {
        try (InputStream is = clazz.getResourceAsStream('/' + clazz.getName().replace('.', '/') + ".class")) {
            final byte[] bytes = getClassBytes(is);
            saveClassBytesToDisk(bytes, clazz.getName());
        } catch (IOException e) {
            logger.error("Couldn't load bytes of " + clazz.getName(), e);
        }
    }

    private static byte[] getClassBytes(InputStream is) throws IOException {
        if (is == null) {
            throw new IOException("Class not found");
        }
        byte[] b = new byte[is.available()];
        int len = 0;
        while (true) {
            int n = is.read(b, len, b.length - len);
            if (n == -1) {
                if (len < b.length) {
                    byte[] c = new byte[len];
                    System.arraycopy(b, 0, c, 0, len);
                    b = c;
                }
                return b;
            }
            len += n;
            if (len == b.length) {
                int last = is.read();
                if (last < 0) {
                    return b;
                }
                byte[] c = new byte[b.length + 1000];
                System.arraycopy(b, 0, c, 0, len);
                c[len++] = (byte) last;
                b = c;
            }
        }
    }

    public static String stripClassName(String classname) {
        final String[] split = classname.split("\\.");
        return split[split.length - 1];
    }

    /**
     * Saves the class bytes in the folder ASM_GTNH\TRANSFORMER_NAME as three files, .class file, bytecode in a text
     * file, and asmified in a text file.
     *
     * @param classBytes       - the bytes of the class to save
     * @param classname        - the class of the name, with '.' to separate package names
     * @param classTransformer - the class transformer that changed the class resulting in the classBytes
     */
    public static void saveClassBytesToDisk(byte[] classBytes, String classname, Object classTransformer) {
        classname = stripClassName(classTransformer.getClass().getName()).toUpperCase() + File.separatorChar
                + classname;
        saveClassBytesToDisk(classBytes, classname);
    }

    /**
     * Saves the class bytes in the folder ASM_GTNH as three files, .class file, bytecode in a text file, and asmified
     * in a text file.
     *
     * @param classBytes - the bytes of the class to save
     * @param classname  - the class of the name, with '.' to separate package names
     */
    public static void saveClassBytesToDisk(byte[] classBytes, String classname) {
        final String fileName = classname.replace('.', File.separatorChar);
        saveAsRawClassFile(classBytes, classname, fileName);
        saveAsBytecodeFile(classBytes, classname, fileName);
        saveAsASMFile(classBytes, classname, fileName);
    }

    /**
     * Saves the byte array to the disk in a .class file, the folder is named after the class transformer
     *
     * @param classBytes       - the bytes of the class to save
     * @param classname        - the class of the name, with '.' to separate package names
     * @param classTransformer - the class transformer that changed the class resulting in the classBytes
     */
    public static void saveAsRawClassFile(byte[] classBytes, String classname, Object classTransformer) {
        final String fileName = stripClassName(classTransformer.getClass().getName()).toUpperCase() + File.separatorChar
                + classname.replace('.', File.separatorChar);
        saveAsRawClassFile(classBytes, classname, fileName);
    }

    /**
     * Saves the byte array to the disk in a .class file
     *
     * @param classBytes - the bytes of the class to save
     * @param classname  - the class of the name, with '.' to separate package names
     */
    public static void saveAsRawClassFile(byte[] classBytes, String classname) {
        final String fileName = classname.replace('.', File.separatorChar);
        saveAsRawClassFile(classBytes, classname, fileName);
    }

    /**
     * Saves the byte array to the disk in a .class file
     *
     * @param classBytes - the bytes of the class to save
     * @param classname  - the class of the name, with '.' to separate package names
     * @param fileName   - the name of the file to save to
     */
    public static void saveAsRawClassFile(byte[] classBytes, String classname, String fileName) {
        if (outputDir == null) {
            emptyClassOutputFolder();
        }
        // Replace $->. because otherwise the files are invisible in IntelliJ
        fileName = fileName.replace('$', '.');
        final File classFile = new File(outputDir, fileName + ".class");
        final File outDir = classFile.getParentFile();
        if (!outDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
        }
        if (classFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            classFile.delete();
        }
        try (final OutputStream output = Files.newOutputStream(classFile.toPath())) {
            output.write(classBytes);
            logger.info("Saved class (byte[]) to " + classFile.toPath());
        } catch (IOException e) {
            logger.error("Could not save class (byte[]) " + classname, e);
        }
    }

    /**
     * Reads the class bytes and saves the bytecode to the disk as a .text file, the folder is named after the class
     * transformer
     *
     * @param classBytes       - the bytes of the class to save
     * @param classname        - the class of the name, with '.' to separate package names
     * @param classTransformer - the class transformer that changed the class resulting in the classBytes
     */
    public static void saveAsBytecodeFile(byte[] classBytes, String classname, Object classTransformer) {
        final String fileName = stripClassName(classTransformer.getClass().getName()).toUpperCase() + File.separatorChar
                + classname.replace('.', File.separatorChar);
        saveAsBytecodeFile(classBytes, classname, fileName);
    }

    /**
     * Reads the class bytes and saves the bytecode to the disk as a .text file
     *
     * @param classBytes - the bytes of the class to save
     * @param classname  - the class of the name, with '.' to separate package names
     */
    public static void saveAsBytecodeFile(byte[] classBytes, String classname) {
        final String fileName = classname.replace('.', File.separatorChar);
        saveAsBytecodeFile(classBytes, classname, fileName);
    }

    /**
     * Reads the class bytes and saves the bytecode to the disk as a .text file
     *
     * @param classBytes - the bytes of the class to save
     * @param classname  - the class of the name, with '.' to separate package names
     * @param fileName   - the name of the file to save to
     */
    public static void saveAsBytecodeFile(byte[] classBytes, String classname, String fileName) {
        if (outputDir == null) {
            emptyClassOutputFolder();
        }
        // Replace $->. because otherwise the files are invisible in IntelliJ
        fileName = fileName.replace('$', '.');
        final File bytecodeFile = new File(outputDir, fileName + "_BYTECODE.txt");
        final File outDir = bytecodeFile.getParentFile();
        if (!outDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
        }
        if (bytecodeFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            bytecodeFile.delete();
        }
        try (final OutputStream output = Files.newOutputStream(bytecodeFile.toPath())) {
            final ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(output)), 0);
            logger.info("Saved class (bytecode) to " + bytecodeFile.toPath());
        } catch (IOException e) {
            logger.error("Could not save class (bytecode) " + classname, e);
        }
    }

    /**
     * Reads the class bytes and saves an ASMIFIED version of the bytecode to the disk as a .text file, the folder is
     * named after the class transformer
     *
     * @param classBytes       - the bytes of the class to save
     * @param classname        - the class of the name, with '.' to separate package names
     * @param classTransformer - the class transformer that changed the class resulting in the classBytes
     */
    public static void saveAsASMFile(byte[] classBytes, String classname, Object classTransformer) {
        final String fileName = stripClassName(classTransformer.getClass().getName()).toUpperCase() + File.separatorChar
                + classname.replace('.', File.separatorChar);
        saveAsASMFile(classBytes, classname, fileName);
    }

    /**
     * Reads the class bytes and saves an ASMIFIED version of the bytecode to the disk as a .text file
     *
     * @param classBytes - the bytes of the class to save
     * @param classname  - the class of the name, with '.' to separate package names
     */
    public static void saveAsASMFile(byte[] classBytes, String classname) {
        final String fileName = classname.replace('.', File.separatorChar);
        saveAsASMFile(classBytes, classname, fileName);
    }

    /**
     * Reads the class bytes and saves an ASMIFIED version of the bytecode to the disk as a .text file
     *
     * @param classBytes - the bytes of the class to save
     * @param classname  - the class of the name, with '.' to separate package names
     * @param fileName   - the name of the file to save to
     */
    public static void saveAsASMFile(byte[] classBytes, String classname, String fileName) {
        if (outputDir == null) {
            emptyClassOutputFolder();
        }
        // Replace $->. because otherwise the files are invisible in IntelliJ
        fileName = fileName.replace('$', '.');
        final File asmifiedFile = new File(outputDir, fileName + "_ASM.txt");
        final File outDir = asmifiedFile.getParentFile();
        if (!outDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
        }
        if (asmifiedFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            asmifiedFile.delete();
        }
        try (final OutputStream output = Files.newOutputStream(asmifiedFile.toPath())) {
            final ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(output)), 0);
            logger.info("Saved class (ASMified) to " + asmifiedFile.toPath());
        } catch (IOException e) {
            logger.error("Could not save class (ASMified) " + classname, e);
        }
    }

}
