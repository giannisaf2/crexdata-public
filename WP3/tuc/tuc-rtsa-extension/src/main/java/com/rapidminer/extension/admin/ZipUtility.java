/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rapidminer.tools.LogService
 *  edu.umd.cs.findbugs.annotations.SuppressFBWarnings
 */
package com.rapidminer.extension.admin;

import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsException;
import com.rapidminer.tools.LogService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipUtility {
    private static final byte[] zipFileBuffer = new byte[0xA00000];
    private static final Logger LOGGER = LogService.getRoot();

    private ZipUtility() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressFBWarnings(value={"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"}, justification="Suppressing null exception warning from Objects.requireNonNull. In this case, listFiles() will return an empty list in case the directory is empty.")
    public static File zip(Path source2, Path destination, List<Path> exclusions) throws AdminToolsException {
        AssertUtility.notNull(source2, "'source' cannot be null");
        AssertUtility.notNull(destination, "'destination' cannot be null");
        File sourceFolder = source2.toFile();
        File zipFile = destination.toFile();
        if (!zipFile.exists()) {
            try {
                boolean created = zipFile.createNewFile();
                if (!created) {
                    LOGGER.severe(() -> String.format("Could not create '%s'", zipFile));
                }
            }
            catch (IOException e) {
                throw new AdminToolsException(String.format("Cannot create '%s'", zipFile.toPath().toAbsolutePath()), e);
            }
        }
        try (FileOutputStream fis = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fis);){
            LOGGER.info(() -> String.format("Started to compress '%s'", sourceFolder));
            Arrays.asList(Objects.requireNonNull(sourceFolder.listFiles())).forEach(f -> {
                try {
                    ZipUtility.addZipEntries(zos, f, "", exclusions);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        catch (IOException e) {
            throw new AdminToolsException(String.format("Cannot compress '%s'", sourceFolder.toPath().toAbsolutePath()), e);
        }
        LOGGER.info(() -> String.format("Created ZIP file '%s' from source '%s'", zipFile, sourceFolder));
        return zipFile;
    }

    @SuppressFBWarnings(value={"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"}, justification="Supressing null exception warning from Objects.requireNonNull. In this case, listFiles() will return an empty list in case the directory is empty.")
    private static void addZipEntries(ZipOutputStream zos, File currentFile, String path, List<Path> exclusions) throws IOException {
        Path file = currentFile.toPath();
        boolean shouldExclude = exclusions.stream().anyMatch(excludedPath -> excludedPath.equals(file));
        if (shouldExclude) {
            return;
        }
        if (!currentFile.isDirectory()) {
            ZipEntry entry = new ZipEntry(path + currentFile.getName());
            zos.putNextEntry(entry);
            try (FileInputStream fis = new FileInputStream(currentFile);){
                while (true) {
                    int realLength;
                    if ((realLength = fis.read(zipFileBuffer)) == -1) {
                        zos.flush();
                        zos.closeEntry();
                        return;
                    }
                    zos.write(zipFileBuffer, 0, realLength);
                }
            }
        }
        Arrays.asList(Objects.requireNonNull(currentFile.listFiles())).forEach(f -> {
            try {
                ZipUtility.addZipEntries(zos, f, path + currentFile.getName() + "/", exclusions);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}

