package tools.canine.backup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import tools.canine.backup.config.BackupConfig;
import tools.canine.backup.types.Docker;
import tools.canine.backup.types.MySQL;
import tools.canine.backup.types.StaticFiles;
import tools.canine.backup.utils.FileUtil;
import tools.canine.backup.utils.RequestUtil;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

public class CanineBackup {

    private static Logger logger;
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
        JSONObject configJson = new JSONObject(configContents);
        setupConfig(configJson);

        // backup the static files!
        logger.info("Starting static files...");
        for (Map.Entry<String, String> entry : config.getStaticFiles().entrySet()) {
            String service = entry.getKey();
            String path = entry.getValue();
            StaticFiles staticFiles = new StaticFiles(service, path);
            staticFiles.backup();
        }
        logger.info("Static files are done!");

        // backup the containers!
        logger.info("Starting docker stacks backup...");
        Docker docker = new Docker(configJson.getString("dockerStacks"));
        docker.backup();
        logger.info("Docker stacks are done!");

        logger.info("Starting MySQL databases backup...");
        JSONObject mysqlConfig = configJson.getJSONObject("mysql");
        JSONArray databasesArray = mysqlConfig.getJSONArray("databases");
        ArrayList<String> databases = new ArrayList<>();
        for (int i = 0; i < databasesArray.length(); i++) {
            databases.add(databasesArray.getString(i));
        }
        MySQL mysql = new MySQL(databases);
        mysql.backup();
        logger.info("MySQL databases are done!");

        logger.info("Everything is done!!");
        RequestUtil.sendAlert("normal", "Backup Complete (" + timeStamp + ")", "All backups have been completed.", "default");
    }

    private static void setupConfig(JSONObject json) {
        logger.info("Setting up config");
        // add the static paths
        JSONObject staticFiles = json.getJSONObject("staticFiles");
        for (String service : staticFiles.keySet()) {
            String path = staticFiles.getString(service);
            config.addPath(service, path);
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

        // add mysql info
        JSONObject mysqlInfo = json.getJSONObject("mysql");
        for (String key : mysqlInfo.keySet()) {
            if (key.equalsIgnoreCase("databases")) continue;
            String value = mysqlInfo.getString(key);
            config.addMysqlInfo(key, value);
        }
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