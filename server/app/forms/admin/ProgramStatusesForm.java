package forms.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;
import services.LocalizedStrings;
import services.program.StatusDefinitions;

/** Form for creating / editing program statuses. */
@Validate
public final class ProgramStatusesForm implements Validatable<List<ValidationError>> {
  public static final String CONFIGURED_STATUS_TEXT_FORM_NAME = "configuredStatusText";
  public static final String STATUS_TEXT_FORM_NAME = "statusText";
  public static final String EMAIL_BODY_FORM_NAME = "emailBody";
  public static final String DEFAULT_CHECKBOX_NAME = "defaultStatusCheckbox";

  public static final ImmutableList<String> FIELD_NAMES =
      ImmutableList.of(
          CONFIGURED_STATUS_TEXT_FORM_NAME,
          STATUS_TEXT_FORM_NAME,
          EMAIL_BODY_FORM_NAME,
          DEFAULT_CHECKBOX_NAME);

  // For edits, this field signifies the status text currently configured
  // for the program. It's used to locate the status that should be edited.
  // Creating a new status is signified by this field being empty.
  private String configuredStatusText;

  private String statusText;

  private String emailBody;

  private Optional<Boolean> defaultStatus;

  public ProgramStatusesForm() {
    this.configuredStatusText = "";
    this.statusText = "";
    this.emailBody = "";
    this.defaultStatus = Optional.of(false);
  }

  /** Initializes a ProgramStatusesForm from the given status object. */
  public static ProgramStatusesForm fromStatus(StatusDefinitions.Status status) {
    ProgramStatusesForm form = new ProgramStatusesForm();
    form.setConfiguredStatusText(status.statusText());
    form.setStatusText(status.statusText());
    // Extract the English email body if email has been configured.
    String emailBody = status.localizedEmailBodyText().map(LocalizedStrings::getDefault).orElse("");
    form.setEmailBody(emailBody);
    form.setDefaultStatus(status.defaultStatus());
    return form;
  }

  public String getConfiguredStatusText() {
    return configuredStatusText;
  }

  public void setConfiguredStatusText(String value) {
    configuredStatusText = checkNotNull(value);
  }

  public String getStatusText() {
    return statusText;
  }

  public void setStatusText(String value) {
    statusText = checkNotNull(value).trim();
  }

  public String getEmailBody() {
    return emailBody;
  }

  public void setEmailBody(String value) {
    emailBody = checkNotNull(value).trim();
  }

  public Optional<Boolean> getDefaultStatus() {
    return defaultStatus;
  }

  public void setDefaultStatus(Optional<Boolean> value) {
    defaultStatus = value;
  }

  @Override
  public List<ValidationError> validate() {
    // The required constraint isn't used on this field since we want to trim
    // the provided input to ensure that "   " isn't considered valid. The
    // Formats.NonEmpty annotation can be used, but it returns null for empty
    // strings.
    if (Strings.isNullOrEmpty(statusText)) {
      return ImmutableList.of(new ValidationError("statusText", "This field is required."));
    }
    return ImmutableList.of();
  }
}
