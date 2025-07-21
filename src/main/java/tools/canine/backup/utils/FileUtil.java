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
                            } catch (NoSuchFileException e) {
                                logger.warn("File disappeared during compression: {}", path);
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

    /**
     * Backup a given file.
     *
     * @param name      The name of the service.
     * @param folder    The folder to upload to.
     * @param localPath The path to back up.
     */
    public static void backupFile(String name, String folder, String localPath) {
        String baseName = name + "_" + CanineBackup.getTimeStamp();
        String prefix = folder + "/" + name + "_";

        // compress the folder we want to do
        String compressedName = baseName + ".zip";
        logger.info("Compressing '{}' to '{}'", localPath, compressedName);
        boolean compress = FileUtil.compress(localPath, compressedName);
        if (compress) {
            logger.info("Finished compressing '{}'", compressedName);
        } else {
            logger.error("Failed to compress '{}'", localPath);
            return;
        }

        // encrypt the zip file
        String encryptedName = compressedName + ".gpg";
        logger.info("Encrypting '{}' to '{}'", compressedName, encryptedName);
        boolean encrypt = GPGUtil.encryptFile(compressedName, encryptedName, true, true);
        if (encrypt) {
            logger.info("Finished encrypting '{}'", encryptedName);
        } else {
            logger.error("Failed to encrypt '{}'", compressedName);
            return;
        }

        // upload the file to s3
        String destination = folder + "/" + encryptedName;
        logger.info("Uploading '{}' to '{}'", encryptedName, destination);
        boolean upload = AWSUtils.uploadFile(encryptedName, destination);
        if (upload) {
            logger.info("Uploaded '{}' to '{}'", encryptedName, destination);
        } else {
            logger.error("Failed to upload '{}'", encryptedName);
            return;
        }

        // send the alert that it was successful
        String title = "Backup Completed (" + name + ")";
        String description = "Backup completed successfully for '" + name + "` at " + CanineBackup.getTimeStamp();
        RequestUtil.sendAlert(title, description, "default");

        // clean old backups
        AWSUtils.clean(prefix, 24);

        // clean up the zipped and encrypted files
        try {
            logger.info("Cleaning up {}", compressedName);
            logger.info("Cleaning up {}", encryptedName);
            Files.delete(Path.of(compressedName));
            Files.delete(Path.of(encryptedName));
        } catch (IOException exception) {
            RequestUtil.sendAlert("Failed Deletion", exception.getMessage(), "high");
            logger.error("Failed to delete group '{}' as '{}'", name, compressedName, exception);
        }
    }

    /**
     * Run a command on the system.
     *
     * @param service The service that ran the command.
     * @param command The command.
     */
    public static void runCommand(String service, String command) {
        try {
            logger.debug("Running command: {}", command);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Failed to run command {} (exit code {})", command, exitCode);
                RequestUtil.sendAlert("Command Fail (" + service + ")", "Command failed: '" + command + "' (exit code " + exitCode + ")", "high");
            }
        } catch (IOException | InterruptedException exception) {
            logger.error("Failed to run command '{}'", command, exception);
            RequestUtil.sendAlert("Command Fail (" + service + ")", exception.getMessage(), "high");
        }
    }
}