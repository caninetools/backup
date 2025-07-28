package tools.canine.backup.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.canine.backup.CanineBackup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {

    private static final Logger logger = LogManager.getLogger(FileUtil.class);

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
    public static boolean compressPath(String pathToCompress, String output) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Path.of(output)))) {
            try (Stream<Path> paths = Files.walk(Path.of(pathToCompress))) {
                paths.filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            try {
                                String zipEntryName = Path.of(pathToCompress).relativize(path).toString().replace("\\", "/");
                                zos.putNextEntry(new ZipEntry(zipEntryName));
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (NoSuchFileException e) {
                                logger.warn("File disappeared during compression: {}", path);
                            } catch (IOException exception) {
                                RequestUtil.sendAlert("failure", "Failed Compression", exception.getMessage(), "max");
                                logger.error("Unable to compress file: {}", path, exception);
                            }
                        });
            }
            return true;
        } catch (IOException exception) {
            RequestUtil.sendAlert("failure", "Failed Compression", exception.getMessage(), "max");
            logger.error("Unable to compress folder: {}", pathToCompress, exception);
            return false;
        }
    }

    /**
     * Compress a single file into a zip archive.
     *
     * @param fileToCompress The file to compress.
     * @param output         The output zip file.
     * @return true if compression succeeded, false otherwise.
     */
    public static boolean compressFile(String fileToCompress, String output) {
        Path inputFile = Path.of(fileToCompress);
        if (!Files.exists(inputFile) || Files.isDirectory(inputFile)) {
            logger.error("Provided path is invalid or not a file: {}", fileToCompress);
            return false;
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Path.of(output)))) {
            String fileName = inputFile.getFileName().toString();
            zos.putNextEntry(new ZipEntry(fileName));
            Files.copy(inputFile, zos);
            zos.closeEntry();
            return true;
        } catch (IOException exception) {
            RequestUtil.sendAlert("failure", "Failed Compression", exception.getMessage(), "max");
            logger.error("Unable to compress file: {}", fileToCompress, exception);
            return false;
        }
    }

    /**
     * Backup a given directory path.
     */
    public static void backupPath(String name, String folder, String localPath) {
        performBackup(name, folder, localPath, false);
    }

    /**
     * Backup a given single file.
     */
    public static void backupFile(String name, String folder, String localFile) {
        performBackup(name, folder, localFile, true);
    }

    /**
     * Perform the full backup: compress, encrypt, upload, alert, and cleanup.
     *
     * @param name   Name of the service
     * @param folder Folder in S3 to upload to
     * @param source The path or file to back up
     * @param isFile True if source is a single file, false if it's a folder
     */
    private static void performBackup(String name, String folder, String source, boolean isFile) {
        String baseName = name + "_" + CanineBackup.getTimeStamp();
        String prefix = folder + "/" + name + "_";

        String compressedName = baseName + ".zip";
        logger.info("Compressing '{}' to '{}'", source, compressedName);
        boolean compress = compressSource(source, compressedName, isFile);
        if (!compress) {
            logger.error("Failed to compress '{}'", source);
            return;
        }

        String encryptedName = compressedName + ".gpg";
        logger.info("Encrypting '{}' to '{}'", compressedName, encryptedName);
        boolean encrypt = GPGUtil.encryptFile(compressedName, encryptedName, true, true);
        if (!encrypt) {
            logger.error("Failed to encrypt '{}'", compressedName);
            return;
        }

        String destination = folder + "/" + encryptedName;
        logger.info("Uploading '{}' to '{}'", encryptedName, destination);
        boolean upload = AWSUtils.uploadFile(encryptedName, destination);
        if (!upload) {
            logger.error("Failed to upload '{}'", encryptedName);
            return;
        }

        RequestUtil.sendAlert("regular", "Backup Completed (" + name + ")", "Backup completed successfully for '" + name + "` at " + CanineBackup.getTimeStamp(), "default");

        AWSUtils.clean(prefix, 24);

        cleanupTempFiles(compressedName, encryptedName, name);
    }

    /**
     * Compress a file or directory based on the mode.
     *
     * @param input  The path to compress
     * @param output Output zip file
     * @param isFile True if input is a file
     * @return true if success
     */
    private static boolean compressSource(String input, String output, boolean isFile) {
        return isFile
                ? FileUtil.compressFile(input, output)
                : FileUtil.compressPath(input, output);
    }

    /**
     * Delete temporary compressed and encrypted files.
     */
    private static void cleanupTempFiles(String compressed, String encrypted, String name) {
        try {
            logger.info("Cleaning up {}", compressed);
            Files.delete(Path.of(compressed));
            logger.info("Cleaning up {}", encrypted);
            Files.delete(Path.of(encrypted));
        } catch (IOException exception) {
            RequestUtil.sendAlert("failure", "Failed Deletion", exception.getMessage(), "high");
            logger.error("Failed to delete backup files for '{}'", name, exception);
        }
    }

    /**
     * Run a command on the system.
     *
     * @param service The service that ran the command.
     * @param command The command.
     * @param envVars Optional environment variables to pass (e.g., MYSQL_PWD)
     */
    public static void runCommand(String service, String command, Map<String, String> envVars) {
        try {
            logger.debug("Running command: {}", command);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            if (envVars != null) {
                pb.environment().putAll(envVars);
            }
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Failed to run command {} (exit code {})", command, exitCode);
                RequestUtil.sendAlert("failure", "Command Fail (" + service + ")", "Command failed: '" + command + "' (exit code " + exitCode + ")", "high");
            }
        } catch (IOException | InterruptedException exception) {
            logger.error("Failed to run command '{}'", command, exception);
            RequestUtil.sendAlert("failure", "Command Fail (" + service + ")", exception.getMessage(), "high");
        }
    }
}