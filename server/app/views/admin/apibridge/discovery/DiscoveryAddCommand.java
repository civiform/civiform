package views.admin.apibridge.discovery;

import lombok.Getter;
import lombok.Setter;
import modules.MainModule;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;

/**
 * Form parameters for adding an api bridge configuration
 *
 * <p>This is a class, not a record, to allow for play form binding
 */
@Getter
@Setter
public final class DiscoveryAddCommand {
  @Required(message = "URL is required")
  @Pattern(value = "^https?://.+", message = "Please enter a valid URL")
  private String hostUrl;

  @Required(message = "Path is required")
  private String urlPath;

  public String buildAdminName() {
    return MainModule.SLUGIFIER.slugify(urlPath);
  }
}
