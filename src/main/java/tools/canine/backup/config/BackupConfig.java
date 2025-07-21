package tools.canine.backup.config;

import java.util.HashMap;
import java.util.Map;

public class BackupConfig {

    private final Map<String, String> staticFiles = new HashMap<>();
    private final Map<String, String> awsInfo = new HashMap<>();
    private final Map<String, String> ntfyInfo = new HashMap<>();
    private String gpgEmail;

    /**
     * Get the file path.
     *
     * @param name The name of the path.
     * @return The path.
     */
    public String getPath(String name) {
        return staticFiles.get(name);
    }

    /**
     * Add a path.
     *
     * @param name The name.
     * @param path The full path.
     */
    public void addPath(String name, String path) {
        staticFiles.put(name, path);
    }

    /**
     * Get the static paths.
     *
     * @return The list of static paths.
     */
    public Map<String, String> getStaticFiles() {
        return staticFiles;
    }

    /**
     * Add AWS information.
     *
     * @param name  The key.
     * @param value The value of the key.
     */
    public void addAwsInfo(String name, String value) {
        awsInfo.put(name, value);
    }

    /**
     * Get AWS info.
     *
     * @param name The name of the key to get.
     * @return The value of said key.
     */
    public String getAwsInfo(String name) {
        return awsInfo.get(name);
    }

    /**
     * Add ntfy information.
     *
     * @param name  The key.
     * @param value The value of the key.
     */
    public void addNtfyInfo(String name, String value) {
        ntfyInfo.put(name, value);
    }

    /**
     * Get ntfy info.
     *
     * @param name The name of the key to get.
     * @return The value of said key.
     */
    public String getNtfyInfo(String name) {
        return ntfyInfo.get(name);
    }

    /**
     * Get the GPG recipient email. This is who the backups are for.
     *
     * @return The email.
     */
    public String getGpgEmail() {
        return gpgEmail;
    }

    /**
     * Set the GPG recipient email for backups.
     *
     * @param gpgEmail The email.
     */
    public void setGpgEmail(String gpgEmail) {
        this.gpgEmail = gpgEmail;
    }
}
