package tools.canine.backup.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.canine.backup.CanineBackup;
import tools.canine.backup.utils.AWSUtils;
import tools.canine.backup.utils.FileUtil;
import tools.canine.backup.utils.GPGUtil;
import tools.canine.backup.utils.RequestUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFiles {

    private final String name;
    private final String localPath;
    private final Logger logger = LogManager.getLogger(this);

    public StaticFiles(String name, String localPath) {
        this.name = name;
        this.localPath = localPath;
    }

    public void backup() {
        // should be name_time
        String baseName = name + "_" + CanineBackup.getTimeStamp();
        String prefix = name + "/" + name + "_";

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
        String destination = name + "/" + encryptedName;
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
}
