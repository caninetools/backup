package tools.canine.backup.config;

import java.util.HashMap;
import java.util.Map;

public class BackupConfig {

    private final Map<String, String> staticFiles = new HashMap<>();
    private final Map<String, String> awsInfo = new HashMap<>();
    private final Map<String, String> ntfyInfo = new HashMap<>();
    private final Map<String, String> mysqlInfo = new HashMap<>();

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
     * Add MySQL information.
     *
     * @param name  The key.
     * @param value The value of the key.
     */
    public void addMysqlInfo(String name, String value) {
        mysqlInfo.put(name, value);
    }

    /**
     * Get MySQL info.
     *
     * @param name The name of the key to get.
     * @return The value of said key.
     */
    public String getMysqlInfo(String name) {
        return mysqlInfo.get(name);
    }
}