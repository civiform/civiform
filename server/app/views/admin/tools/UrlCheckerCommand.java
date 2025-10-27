package views.admin.tools;

import lombok.Data;
import play.data.validation.Constraints;

@Data
public final class UrlCheckerCommand {
  @Constraints.Required(message = "URL is required")
  private String url;
}
