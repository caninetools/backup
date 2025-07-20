package tools.canine.backup.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {

    private static final Logger logger = LogManager.getLogger(FileUtil.class);

    /**
     * Write contents of an object to a file.
     *
     * @param content The content to write.
     * @param file    The file to write to.
     */
    public static void writeFile(Object content, File file) {
        logger.info("Writing to file {}", file.getAbsolutePath());
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(content.toString());
            writer.close();
        } catch (IOException exception) {
            logger.error("Unable to write file {}", file, exception);
        }
    }

    /**
     * Read contents of a file.
     *
     * @param file The file to read.
     * @return The contents. NULL if something went wrong.
     */
    public static String readFile(File file) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(file.toPath());
        } catch (IOException exception) {
            logger.error("Unable to read file {}", file, exception);
            return null;
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /**
     * Compress a given path into a zip.
     *
     * @param pathToCompress The path to compress.
     * @param output         The output zip file.
     * @return true if worked, false if failed.
     */
    public static boolean compress(String pathToCompress, String output) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Path.of(output)))) {
            try (Stream<Path> paths = Files.walk(Path.of(pathToCompress))) {
                paths.filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            try {
                                String zipEntryName = Path.of(pathToCompress).relativize(path).toString().replace("\\", "/");
                                zos.putNextEntry(new ZipEntry(zipEntryName));
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException exception) {
                                RequestUtil.sendAlert("Failed Compression", exception.getMessage(), "high");
                                logger.error("Unable to compress file: {}", path, exception);
                            }
                        });
            }
            return true;
        } catch (IOException exception) {
            RequestUtil.sendAlert("Failed Compression", exception.getMessage(), "high");
            logger.error("Unable to compress folder: {}", pathToCompress, exception);
            return false;
        }
    }
}