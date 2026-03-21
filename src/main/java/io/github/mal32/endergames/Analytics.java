package io.github.mal32.endergames;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;

public class Analytics extends AbstractModule {
  public Analytics(EnderGames plugin) {
    super(plugin);
  }

  public void sendAsync(String event, Map<String, String> props) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> send(event, props));
  }

  private void send(String event, Map<String, String> props) {
    if (EnderGames.isInDebugMode()) return;
    if (!plugin.getConfig().getBoolean("analytics.enabled")) return;

    String id = plugin.getConfig().getString("analytics.id");
    if (id == null) {
      id = UUID.randomUUID().toString();
      plugin.getConfig().set("analytics.id", id);
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.saveConfig());
    }

    final String POSTHOG_PUBLIC_API_KEY = "phc_410WhGEu4C930lfm2TAFRmucTFYJsUXyYxjOLzYCUNA";
    final String POSTHOG_HOST = "eu.i.posthog.com";
    String iso8601Time = Instant.now().toString();

    // I know its dirty but this frees us from a new dependency
    String jsonBody =
        String.format(
            "{\"api_key\": \"%s\", \"event\": \"%s\", \"timestamp\": \"%s\", \"properties\":"
                + " {\"distinct_id\": \"%s\"",
            POSTHOG_PUBLIC_API_KEY, event, iso8601Time, id);
    if (props != null) {
      for (Map.Entry<String, String> entry : props.entrySet()) {
        jsonBody += String.format(", \"%s\": \"%s\"", entry.getKey(), entry.getValue());
      }
    }
    jsonBody += "}}";

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("https://" + POSTHOG_HOST + "/capture"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(jsonBody))
            .build();

    try {
      client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception ex) {
      plugin.getComponentLogger().warn("Could not send analytics: " + ex.toString());
    }
  }
}
