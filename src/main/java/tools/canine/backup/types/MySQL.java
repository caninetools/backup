package tools.canine.backup.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.canine.backup.CanineBackup;
import tools.canine.backup.utils.FileUtil;
import tools.canine.backup.utils.RequestUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MySQL {

    private final ArrayList<String> databases;
    private final Logger logger = LogManager.getLogger(this);

    public MySQL(ArrayList<String> databases) {
        this.databases = databases;
    }

    public void backup() {
        for (String database : databases) {
            String databaseDump = database + ".sql";
            String username = CanineBackup.getConfig().getMysqlInfo("username");
            String password = CanineBackup.getConfig().getMysqlInfo("password");

            // run the dump command
            String command = "mysqldump -u " + username + " " + database + " > " + databaseDump;
            Map<String, String> env = new HashMap<>();
            env.put("MYSQL_PWD", password);
            FileUtil.runCommand(database, command, env);

            // if the dump failed, skip
            if (!Files.exists(Path.of(databaseDump))) {
                logger.warn("{} does not exist after dump, did it fail?", databaseDump);
                continue;
            }

            logger.info("Dumped database '{}' to '{}'", database, databaseDump);
            // backup!!
            FileUtil.backupFile(database, "databases", databaseDump);

            try {
                logger.info("Cleaning up {}", databaseDump);
                Files.delete(Path.of(databaseDump));
            } catch (IOException exception) {
                RequestUtil.sendAlert("Failed Deletion", exception.getMessage(), "high");
                logger.error("Failed to delete backup files for '{}'", databaseDump, exception);
            }
        }
    }
}
