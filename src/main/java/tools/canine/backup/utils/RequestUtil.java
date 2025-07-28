package tools.canine.backup.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.canine.backup.CanineBackup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RequestUtil {

    private static final Logger logger = LogManager.getLogger(RequestUtil.class);

    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Send ntfy message.
     *
     * @param type     The type of alert, good or bad.
     * @param title    Title of message.
     * @param body     Description of message.
     * @param priority The priority of the message. (min,low,default,high,max)
     */
    public static void sendAlert(String type, String title, String body, String priority) {
        String topic = null;
        if (type.equalsIgnoreCase("normal")) {
            topic = CanineBackup.getConfig().getNtfyInfo("normal-alerts");
        }
        if (type.equalsIgnoreCase("failure")) {
            topic = CanineBackup.getConfig().getNtfyInfo("failure-alerts");
        }
        if (topic == null) {
            logger.error("Unable to send alert, topic is null.");
            return;
        }

        String token = CanineBackup.getConfig().getNtfyInfo("token");
        HttpRequest alertRequest = HttpRequest.newBuilder()
                .uri(URI.create(topic))
                .header("Authorization", "Bearer " + token)
                .header("Title", title)
                .header("Priority", priority)
                .header("User-Agent", CanineBackup.getUserAgent())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = client.send(alertRequest, HttpResponse.BodyHandlers.ofString());
            logger.info("Sent ntfy message to topic {} with title '{}': Response {} - {}", topic, title, response.statusCode(), response.body());
        } catch (IOException | InterruptedException exception) {
            logger.error("Unable to send ntfy alert", exception);
        }
    }
}
