package views.admin.apibridge.discovery;

import lombok.Getter;
import lombok.Setter;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;

/**
 * Parameters to perform the api discovery
 *
 * <p>This is a class, not a record, to allow for play form binding
 */
@Getter
@Setter
public final class DiscoverySearchCommand {
  @Required(message = "URL is required")
  @Pattern(value = "^https?://.+", message = "Please enter a valid URL")
  private String hostUrl;
}
