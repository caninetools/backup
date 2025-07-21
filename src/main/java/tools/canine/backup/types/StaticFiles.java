package tools.canine.backup.types;

import tools.canine.backup.utils.FileUtil;

public class StaticFiles {

    private final String name;
    private final String localPath;

    public StaticFiles(String name, String localPath) {
        this.name = name;
        this.localPath = localPath;
    }

    public void backup() {
        // backup the path!
        FileUtil.backupFile(name, name, localPath);
    }
}
