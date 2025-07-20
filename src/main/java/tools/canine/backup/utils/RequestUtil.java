package tools.canine.backup.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
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
     * @param title    Title of message.
     * @param body     Description of message.
     * @param priority The priority of the message. (min,low,default,high,max)
     */
    public static void sendAlert(String title, String body, String priority) {
        JSONObject config = CanineBackup.getConfig().getJSONObject("ntfy");
        String topic = config.getString("topic");
        String token = config.getString("token");
        HttpRequest alertRequest = HttpRequest.newBuilder()
                .uri(URI.create(topic))
                .header("Authorization", "Bearer " + token)
                .header("Title", title)
                .header("Priority", priority)
                .header("User-Agent", CanineBackup.getUserAgent())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            client.send(alertRequest, HttpResponse.BodyHandlers.ofString());
            logger.info("Sent ntfy message to topic {} with title '{}'", topic, title);
        } catch (IOException | InterruptedException exception) {
            logger.error("Unable to send ntfy alert", exception);
        }
    }
}
