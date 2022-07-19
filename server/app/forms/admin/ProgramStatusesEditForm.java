package forms.admin;

import com.google.common.collect.ImmutableList;
import java.util.List;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;

/** Form for creating / editing program statuses. */
@Validate
public final class ProgramStatusesEditForm implements Validatable<List<ValidationError>> {
  public static final String ORIGINAL_STATUS_TEXT_FORM_NAME = "originalStatusText";
  public static final String STATUS_TEXT_FORM_NAME = "statusText";
  public static final String EMAIL_BODY_FORM_NAME = "emailBody";

  public static final ImmutableList<String> FIELD_NAMES =
      ImmutableList.of(ORIGINAL_STATUS_TEXT_FORM_NAME, STATUS_TEXT_FORM_NAME, EMAIL_BODY_FORM_NAME);

  private String originalStatusText;

  @Constraints.Required(message = "This field is required.")
  private String statusText;

  private String emailBody;

  public ProgramStatusesEditForm() {
    this.originalStatusText = "";
    this.statusText = "";
    this.emailBody = "";
  }

  public String getOriginalStatusText() {
    return originalStatusText != null ? originalStatusText : "";
  }

  public void setOriginalStatusText(String value) {
    if (value != null) {
      value = value.trim();
    }
    originalStatusText = value;
  }

  public String getStatusText() {
    return statusText != null ? statusText : "";
  }

  public void setStatusText(String value) {
    if (value != null) {
      value = value.trim();
    }
    statusText = value;
  }

  public String getEmailBody() {
    return emailBody != null ? emailBody : "";
  }

  public void setEmailBody(String value) {
    if (value != null) {
      value = value.trim();
    }
    emailBody = value;
  }

  @Override
  public List<ValidationError> validate() {
    ImmutableList.Builder<ValidationError> errors = ImmutableList.builder();
    if (originalStatusText == null) {
      errors.add(new ValidationError("originalStatusText", "Missing field."));
    }
    if (emailBody == null) {
      errors.add(new ValidationError("emailBody", "Missing field."));
    }
    return errors.build();
  }
}
