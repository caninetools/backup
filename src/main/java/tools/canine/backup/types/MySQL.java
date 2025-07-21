package tools.canine.backup.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class MySQL {

    private final ArrayList<String> databases;
    private final Logger logger = LogManager.getLogger(this);

    public MySQL(ArrayList<String> databases) {
        this.databases = databases;
    }

    public void backup() {
        for (String database : databases) {

        }
    }
}
