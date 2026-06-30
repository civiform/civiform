package views.tags.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import views.tags.AbstractElementModelProcessor.AttributeInfo;

@Getter
@Setter
@Accessors(fluent = true)
public final class AlertFormSettings {
  private final AttributeInfo id;
  private final AttributeInfo alertType;
  private final AttributeInfo slim;
  private final AttributeInfo noIcon;

  @Builder
  public AlertFormSettings(
      AttributeInfo id, AttributeInfo alertType, AttributeInfo slim, AttributeInfo noIcon) {
    this.id = id;
    this.alertType = alertType;
    this.slim = slim;
    this.noIcon = noIcon;
  }
}
