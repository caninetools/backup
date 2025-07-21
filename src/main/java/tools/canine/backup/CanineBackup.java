package tools.canine.backup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import tools.canine.backup.config.BackupConfig;
import tools.canine.backup.types.StaticFiles;
import tools.canine.backup.utils.FileUtil;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class CanineBackup {

    public static Logger logger;
    private static BackupConfig config;
    private static String timeStamp;
    private static final String USER_AGENT = "caninebackup (+https://github.com/caninetools/backup)";

    public static void main(String[] args) {
        timeStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        System.setProperty("log4j.configurationFile", "log4j2config.xml");
        logger = LogManager.getLogger(CanineBackup.class);
        logger.info("Time is {}", timeStamp);

        File configFile = new File("config.json");
        if (!configFile.exists()) {
            logger.error("Config file does not exist");
            System.exit(1);
        }
        String configContents = FileUtil.readFile(configFile);
        if (configContents == null) {
            logger.error("Config file exists, but has no contents");
            System.exit(1);
        }

        // set up the config
        config = new BackupConfig();
        setupConfig(new JSONObject(configContents));

        // handle the static paths
        for (Map.Entry<String, String> entry : config.getStaticFiles().entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue();
            StaticFiles staticFiles = new StaticFiles(name, path);
            staticFiles.backup();
        }
    }

    private static void setupConfig(JSONObject json) {
        logger.info("Setting up config");
        // add the static paths
        JSONObject staticFiles = json.getJSONObject("staticFiles");
        for (String group : staticFiles.keySet()) {
            String path = staticFiles.getString(group);
            config.addPath(group, path);
        }

        // add aws info
        JSONObject awsInfo = json.getJSONObject("aws");
        for (String key : awsInfo.keySet()) {
            String value = awsInfo.getString(key);
            config.addAwsInfo(key, value);
        }

        // add ntfy info
        JSONObject ntfyInfo = json.getJSONObject("ntfy");
        for (String key : ntfyInfo.keySet()) {
            String value = ntfyInfo.getString(key);
            config.addNtfyInfo(key, value);
        }

        config.setGpgEmail(json.getString("gpgEmail"));
    }

    public static BackupConfig getConfig() {
        return config;
    }

    public static String getTimeStamp() {
        return timeStamp;
    }

    public static String getUserAgent() {
        return USER_AGENT;
    }
}