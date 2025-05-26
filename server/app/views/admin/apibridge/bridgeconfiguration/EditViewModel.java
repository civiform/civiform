package views.admin.apibridge.bridgeconfiguration;

import java.time.Instant;
import models.ApiBridgeConfigurationModel;
import org.jetbrains.annotations.NotNull;
import play.data.DynamicForm;

public record EditViewModel(
    Long id,
    String hostUri,
    String uriPath,
    String compatibilityLevel,
    String description,
    String requestSchema,
    String requestSchemaChecksum,
    String responseSchema,
    String responseSchemaChecksum,
    String globalBridgeDefinition,
    boolean isEnabled,
    Instant createTime,
    Instant updateTime) {
  public static EditViewModel create(@NotNull ApiBridgeConfigurationModel item) {
    return new EditViewModel(
        item.getId(),
        item.getHostUri(),
        item.getUriPath(),
        item.getCompatibilityLevel(),
        item.getDescription(),
        item.getRequestSchema(),
        item.getRequestSchemaChecksum(),
        item.getResponseSchema(),
        item.getResponseSchemaChecksum(),
        item.getGlobalBridgeDefinition(),
        item.isEnabled(),
        item.getCreateTime(),
        item.getUpdateTime());
  }

  public static EditViewModel create(DynamicForm form) {
    return new EditViewModel(
        Long.parseLong(form.get("id")),
        form.get("hostUri"),
        form.get("uriPath"),
        form.get("compatibilityLevel"),
        form.get("description"),
        form.get("requestSchema"),
        form.get("requestSchemaChecksum"),
        form.get("responseSchema"),
        form.get("responseSchemaChecksum"),
        form.get("globalBridgeDefinition"),
        Boolean.parseBoolean(form.get("isEnabled")),
        Instant.now(),
        Instant.now());
  }
}
