package views.admin.apikeys;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class ApiKeyNewOnePageViewModel implements BaseViewModel {
  private final boolean hasPrograms;
  private final ImmutableList<ProgramCheckboxData> programs;

  /** Form field values (for re-populating on validation error). */
  private final String keyNameValue;

  private final String expirationValue;
  private final String subnetValue;

  /** Field-level error messages. */
  private final Optional<String> keyNameError;

  private final Optional<String> expirationError;
  private final Optional<String> subnetError;
  private final Optional<String> programsError;

  /** Data for a program checkbox in the form. */
  public record ProgramCheckboxData(String name, String slug, String fieldName, boolean checked) {}

  public String formActionUrl() {
    return controllers.admin.routes.AdminApiKeysController.create().url();
  }
}
