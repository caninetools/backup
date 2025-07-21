package tools.canine.backup.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.canine.backup.CanineBackup;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AWSUtils {

    private static final Logger logger = LogManager.getLogger(AWSUtils.class);

    public static boolean uploadFile(String toUpload, String destination) {
        String endPoint = CanineBackup.getConfig().getAwsInfo("endpoint").replaceAll("/+$", "");
        String accessKey = CanineBackup.getConfig().getAwsInfo("accessKey");
        String secretKey = CanineBackup.getConfig().getAwsInfo("secretKey");
        String bucket = CanineBackup.getConfig().getAwsInfo("bucket");

        try {
            S3Client s3 = S3Client.builder()
                    .endpointOverride(URI.create(endPoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .region(Region.US_EAST_1)
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build())
                    .build();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(destination)
                    .build();

            s3.putObject(putObjectRequest, RequestBody.fromFile(Paths.get(toUpload)));
            s3.close();
            return true;
        } catch (Exception exception) {
            RequestUtil.sendAlert("Failed Upload", exception.getMessage(), "high");
            logger.error("Unable to upload file", exception);
            return false;
        }
    }

    public static void clean(String prefix, int keepCount) {
        String endPoint = CanineBackup.getConfig().getAwsInfo("endpoint").replaceAll("/+$", "");
        String accessKey = CanineBackup.getConfig().getAwsInfo("accessKey");
        String secretKey = CanineBackup.getConfig().getAwsInfo("secretKey");
        String bucket = CanineBackup.getConfig().getAwsInfo("bucket");

        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endPoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build()) {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            List<S3Object> allFiles = new ArrayList<>(s3.listObjectsV2(listRequest).contents());

            allFiles.sort((a, b) -> b.lastModified().compareTo(a.lastModified()));
            if (allFiles.size() <= keepCount) {
                logger.info("No old backups to delete; total backups: {}", allFiles.size());
                return;
            }

            List<S3Object> toDelete = allFiles.subList(keepCount, allFiles.size());

            for (S3Object object : toDelete) {
                logger.info("Deleting old backup: {}", object.key());
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(object.key())
                        .build());
            }

            logger.info("Deleted {} old backup(s)", toDelete.size());

        } catch (Exception exception) {
            RequestUtil.sendAlert("Cleanup Failed", exception.getMessage(), "high");
            logger.error("Failed to clean up old backups", exception);
        }
    }
}