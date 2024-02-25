package services.ai;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http.Request;
import services.settings.SettingsManifest;

public class OpenAIClient implements WSBodyReadables {

  private final WSClient ws;
  private String OPENAI_API_KEY;
  private SettingsManifest settingsManifest;
  private static final String url = "https://api.openai.com/v1/chat/completions";
  private static final String CONTENT_TYPE = "application/json";

  @Inject
  public OpenAIClient(Config configuration, WSClient ws, SettingsManifest settingsManifest) {
    this.ws = checkNotNull(ws);
    this.OPENAI_API_KEY = configuration.getString("openai_api_key");
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public String getRewordedQuestion(Request request, String question) {
    String systemPrompt = settingsManifest.getRewordQuestionSystemPrompt(request).get();
    String model = settingsManifest.getOpenaiModel(request).get();
    WSRequest req = ws.url(url).setContentType(CONTENT_TYPE).addHeader("Authorization", String.format("Bearer %s", OPENAI_API_KEY));
    String data = String.format("{\"model\": \"%s\", \"messages\": [{\"role\": \"system\",\"content\": \"%s\"},{\"role\":\"user\",\"content\":\"%s\"}]}", model, systemPrompt, question);
    WSResponse response = req.post(data).toCompletableFuture().join();
    if (response.getStatus() != 200) {
      return "Error";
    }
    JsonNode json = response.getBody(json());
    return json.at("/choices/0/message/content").asText();
  }
}
