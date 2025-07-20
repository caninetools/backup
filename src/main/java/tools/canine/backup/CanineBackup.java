package tools.canine.backup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import tools.canine.backup.types.StaticFiles;
import tools.canine.backup.utils.FileUtil;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CanineBackup {

    public static Logger logger;
    private static JSONObject config;
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

        config = new JSONObject(configContents);
        JSONObject staticGroups = config.getJSONObject("staticFiles");
        for (String groupName : staticGroups.keySet()) {
            String path = staticGroups.getString(groupName);
            StaticFiles staticFiles = new StaticFiles(groupName, path);
            staticFiles.backup();
        }
    }

    public static JSONObject getConfig() {
        return config;
    }

    public static String getTimeStamp() {
        return timeStamp;
    }

    public static String getUserAgent() {
        return USER_AGENT;
    }
}