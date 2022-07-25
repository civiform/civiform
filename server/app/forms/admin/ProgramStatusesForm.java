package forms.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;
import services.program.StatusDefinitions;

/** Form for creating / editing program statuses. */
@Validate
public final class ProgramStatusesForm implements Validatable<List<ValidationError>> {
  public static final String CONFIGURED_STATUS_TEXT_FORM_NAME = "configuredStatusText";
  public static final String STATUS_TEXT_FORM_NAME = "statusText";
  public static final String EMAIL_BODY_FORM_NAME = "emailBody";

  public static final ImmutableList<String> FIELD_NAMES =
      ImmutableList.of(
          CONFIGURED_STATUS_TEXT_FORM_NAME, STATUS_TEXT_FORM_NAME, EMAIL_BODY_FORM_NAME);

  // For edits, this field signifies the status text currently configured
  // for the program. It's used to locate the status that should be edited.
  // Creating a new status is signified by this field being empty.
  private String configuredStatusText;

  private String statusText;

  private String emailBody;

  public ProgramStatusesForm() {
    this.configuredStatusText = "";
    this.statusText = "";
    this.emailBody = "";
  }

  /** Initializes a ProgramStatusesForm from the given status object. */
  public static ProgramStatusesForm fromStatus(StatusDefinitions.Status status) {
    ProgramStatusesForm form = new ProgramStatusesForm();
    form.setConfiguredStatusText(status.statusText());
    form.setStatusText(status.statusText());
    form.setEmailBody(status.emailBodyText().orElse(""));
    return form;
  }

  public String getConfiguredStatusText() {
    return checkNotNull(configuredStatusText);
  }

  public void setConfiguredStatusText(String value) {
    configuredStatusText = checkNotNull(value).trim();
  }

  public String getStatusText() {
    return checkNotNull(statusText);
  }

  public void setStatusText(String value) {
    statusText = checkNotNull(value).trim();
  }

  public String getEmailBody() {
    return checkNotNull(emailBody);
  }

  public void setEmailBody(String value) {
    emailBody = checkNotNull(value).trim();
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
