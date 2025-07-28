package tools.canine.backup.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.canine.backup.utils.FileUtil;
import tools.canine.backup.utils.RequestUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class Docker {

    private final String stacksRoot;
    private final Logger logger = LogManager.getLogger(this);

    public Docker(String stacksRoot) {
        this.stacksRoot = stacksRoot;
    }

    public void backup() {
        // list all directories in the stacks folder
        List<String> containers;
        try (Stream<Path> paths = Files.list(Path.of(stacksRoot))) {
            containers = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .toList();
        } catch (IOException exception) {
            RequestUtil.sendAlert("failure", "Failed Docker", exception.getMessage(), "high");
            logger.error("Unable to get stacks folder", exception);
            return;
        }

        // stop, backup, start all containers
        logger.info("Containers: {}", containers);
        for (String stack : containers) {
            stopContainer(stack);
            FileUtil.backupPath(stack, "docker", stacksRoot + "/" + stack);
            startContainer(stack);
        }
    }

    public void stopContainer(String stack) {
        String stackPath = stacksRoot + "/" + stack;
        String command = "sudo docker compose -f \"" + stackPath + "/compose.yaml\" stop";

        logger.info("Stopping stack: {}", stack);
        FileUtil.runCommand(stack, command, null);
    }

    public void startContainer(String stack) {
        String stackPath = stacksRoot + "/" + stack;
        String command = "sudo docker compose -f \"" + stackPath + "/compose.yaml\" start";

        logger.info("Starting stack: {}", stack);
        FileUtil.runCommand(stack, command, null);
    }
}
