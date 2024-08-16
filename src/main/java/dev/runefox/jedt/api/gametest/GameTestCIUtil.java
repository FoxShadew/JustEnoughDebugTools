package dev.runefox.jedt.api.gametest;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @deprecated Use Fabric's testing API.
 */
@Deprecated(forRemoval = true)
public class GameTestCIUtil {
    private static final Logger LOGGER = LogManager.getLogger();

    @Deprecated(forRemoval = true)
    public static void exportTestWorldAsZip(MinecraftServer server, File outputFile) {
        Path path = server.getWorldPath(LevelResource.ROOT).toAbsolutePath();
        outputFile.getAbsoluteFile().getParentFile().mkdirs();

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
             Stream<Path> walkFiles = Files.walk(path)) {

            walkFiles.forEachOrdered(subpath -> {
                if (!Files.isRegularFile(subpath)) {
                    return;
                }
                try {
                    Path rel = path.relativize(subpath);
                    String zipEntryName = rel.toString();
                    ZipEntry entry = new ZipEntry(zipEntryName);
                    entry.setLastModifiedTime(Files.getLastModifiedTime(subpath));

                    out.putNextEntry(entry);
                    try (InputStream in = new FileInputStream(subpath.toFile())) {
                        IOUtils.copy(in, out);
                    }
                    out.closeEntry();
                    out.flush();
                } catch (IOException exc) {
                    throw new RuntimeException("Failed to save world to ZIP file", exc);
                }
            });
        } catch (IOException exc) {
            throw new RuntimeException("Failed to save world to ZIP file", exc);
        }

        LOGGER.info("Saved test world as ZIP file at " + outputFile.getAbsoluteFile());
    }
}
