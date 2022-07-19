package forms.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Validatable;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;
import play.mvc.Http;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;

/** Form for creating / editing program statuses. */
public final class ProgramStatusesEditForm {
  public static final String ORIGINAL_STATUS_TEXT_FORM_NAME = "originalStatusText";
  public static final String STATUS_TEXT_FORM_NAME = "statusText";
  public static final String EMAIL_BODY_FORM_NAME = "emailBody";

  private final Pair<Form<EditFormValues>, Optional<StatusDefinitions>> validatedResult;

  ProgramStatusesEditForm(Pair<Form<EditFormValues>, Optional<StatusDefinitions>> validatedResult) {
    this.validatedResult = checkNotNull(validatedResult);
  }

  @Validate
  public static final class EditFormValues implements Validatable<List<ValidationError>> {
    private String originalStatusText;

    @Constraints.Required(message = "This field is required.")
    private String statusText;

    private String emailBody;

    public EditFormValues() {}

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

  public boolean hasErrors() {
    return validatedResult.getLeft().hasErrors();
  }

  public Form<EditFormValues> form() {
    return validatedResult.getLeft();
  }

  public EditFormValues rawFormValues() {
    return validatedResult.getLeft().value().orElse(new EditFormValues());
  }

  public StatusDefinitions getEditedStatuses() {
    if (hasErrors() || validatedResult.getRight().isEmpty()) {
      throw new IllegalStateException("invalid request");
    }
    return validatedResult.getRight().get();
  }

  public static ProgramStatusesEditForm fromRequest(
      Http.Request request, ProgramDefinition program, FormFactory formFactory) {
    return new ProgramStatusesEditForm(
        validate(
            program,
            formFactory
                .form(EditFormValues.class)
                .bindFromRequest(
                    request,
                    ORIGINAL_STATUS_TEXT_FORM_NAME,
                    STATUS_TEXT_FORM_NAME,
                    EMAIL_BODY_FORM_NAME)));
  }

  private static Pair<Form<EditFormValues>, Optional<StatusDefinitions>> validate(
      ProgramDefinition program, Form<EditFormValues> form) {
    if (form.value().isEmpty()) {
      return Pair.of(form, Optional.empty());
    }
    EditFormValues values = form.value().get();
    // TODO(#2752): This is messy, do another pass on it once more is fleshed out.
    StatusDefinitions current = program.statusDefinitions();
    int existingStatusIndex = matchingStatusIndex(values.getStatusText(), current);
    int originalStatusIndex = matchingStatusIndex(values.getOriginalStatusText(), current);
    if (values.getOriginalStatusText().isEmpty()) {
      if (existingStatusIndex != -1) {
        form =
            form.withGlobalError(
                String.format("A status with name %s already exists", values.getStatusText()));
      }
    } else {
      if (originalStatusIndex == -1) {
        form =
            form.withGlobalError(
                "The status being edited no longer exists and may have been modified in a separate"
                    + " window.");
      } else if (originalStatusIndex != existingStatusIndex && existingStatusIndex != -1) {
        form =
            form.withGlobalError(
                String.format("A status with the name %s already exists", values.getStatusText()));
      }
    }

    if (form.hasErrors()) {
      return Pair.of(form, Optional.empty());
    }

    List<StatusDefinitions.Status> statusesForUpdate =
        current.getStatuses().stream().collect(Collectors.toList());
    if (originalStatusIndex != -1) {
      StatusDefinitions.Status preExistingStatus = statusesForUpdate.get(originalStatusIndex);
      statusesForUpdate.set(
          originalStatusIndex,
          StatusDefinitions.Status.builder()
              .setStatusText(values.getStatusText())
              .setLocalizedStatusText(preExistingStatus.localizedStatusText())
              .setEmailBodyText(values.getEmailBody())
              .setLocalizedEmailBodyText(
                  preExistingStatus
                      .localizedEmailBodyText()
                      .orElse(LocalizedStrings.withDefaultValue(values.getEmailBody())))
              .build());
    } else {
      statusesForUpdate.add(
          StatusDefinitions.Status.builder()
              .setStatusText(values.getStatusText())
              .setLocalizedStatusText(LocalizedStrings.withDefaultValue(values.getStatusText()))
              .setEmailBodyText(values.getEmailBody())
              .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue(values.getEmailBody()))
              .build());
    }
    current.setStatuses(ImmutableList.copyOf(statusesForUpdate));

    return Pair.of(form, Optional.of(current));
  }

  private static int matchingStatusIndex(String statusText, StatusDefinitions statuses) {
    for (int i = 0; i < statuses.getStatuses().size(); i++) {
      if (statuses
          .getStatuses()
          .get(i)
          .statusText()
          .toLowerCase()
          .equals(statusText.toLowerCase())) {
        return i;
      }
    }
    return -1;
  }
}
