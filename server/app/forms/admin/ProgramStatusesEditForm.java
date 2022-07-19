package forms.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import controllers.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;

/** Form for creating / editing program statuses. */
public final class ProgramStatusesEditForm {
  public static final String ORIGINAL_STATUS_TEXT_FORM_NAME = "original_status_text";
  public static final String STATUS_TEXT_FORM_NAME = "status_text";
  public static final String EMAIL_BODY_FORM_NAME = "email_body";

  private final DynamicForm initialForm;
  private final ProgramDefinition program;
  private Pair<DynamicForm, Optional<StatusDefinitions>> validatedResult;

  public ProgramStatusesEditForm(
      ProgramDefinition program, FormFactory formFactory, Http.Request request) {
    this.initialForm = checkNotNull(formFactory).form().bindFromRequest(request);
    this.program = program;
  }

  public boolean hasErrors() {
    return getValidatedResult().getLeft().hasErrors();
  }

  public DynamicForm validatedForm() {
    return getValidatedResult().getLeft();
  }

  public StatusDefinitions getEditedStatuses() {
    if (hasErrors()) {
      throw new IllegalStateException("invalid request");
    }
    return getValidatedResult().getRight().get();
  }

  private Pair<DynamicForm, Optional<StatusDefinitions>> getValidatedResult() {
    if (validatedResult != null) {
      return validatedResult;
    }
    validatedResult = validateInternal(initialForm, program);
    return validatedResult;
  }

  private static Pair<DynamicForm, Optional<StatusDefinitions>> validateInternal(
      DynamicForm initialForm, ProgramDefinition program) {
    DynamicForm validatedForm = initialForm.discardingErrors();

    String rawStatusText = initialForm.get(STATUS_TEXT_FORM_NAME);
    if (rawStatusText == null) {
      throw new BadRequestException(String.format("missing %s", STATUS_TEXT_FORM_NAME));
    }
    String rawEmailBody = initialForm.get(EMAIL_BODY_FORM_NAME);
    if (rawEmailBody == null) {
      throw new BadRequestException(String.format("missing %s", EMAIL_BODY_FORM_NAME));
    }
    String originalStatusText = initialForm.get(ORIGINAL_STATUS_TEXT_FORM_NAME);
    if (originalStatusText == null) {
      throw new BadRequestException(String.format("missing %s", ORIGINAL_STATUS_TEXT_FORM_NAME));
    }

    if (rawStatusText.isEmpty()) {
      validatedForm = validatedForm.withError(STATUS_TEXT_FORM_NAME, "This field is required");
    }
    final String statusText = rawStatusText.trim();
    final String emailBody = rawEmailBody.trim();

    // TODO(#2752): This is messy, do another pass on it once more is fleshed out.
    StatusDefinitions current = program.statusDefinitions();
    int existingStatusIndex = matchingStatusIndex(statusText, current);
    int originalStatusIndex = matchingStatusIndex(originalStatusText, current);
    if (originalStatusText.isEmpty()) {
      if (existingStatusIndex != -1) {
        validatedForm =
            validatedForm.withGlobalError(
                String.format("A status with name %s already exists", statusText));
      }
    } else {
      if (originalStatusIndex == -1) {
        validatedForm =
            validatedForm.withGlobalError(
                "The status being edited no longer exists and may have been modified in a separate"
                    + " window.");
      } else if (originalStatusIndex != existingStatusIndex && existingStatusIndex != -1) {
        validatedForm =
            validatedForm.withGlobalError(
                String.format("A status with the name %s already exists", statusText));
      }
    }

    if (validatedForm.hasErrors()) {
      return Pair.of(validatedForm, Optional.empty());
    }

    List<StatusDefinitions.Status> statusesForUpdate =
        current.getStatuses().stream().collect(Collectors.toList());
    if (originalStatusIndex != -1) {
      StatusDefinitions.Status preExistingStatus = statusesForUpdate.get(originalStatusIndex);
      statusesForUpdate.set(
          originalStatusIndex,
          StatusDefinitions.Status.builder()
              .setStatusText(statusText)
              .setLocalizedStatusText(preExistingStatus.localizedStatusText())
              .setEmailBodyText(emailBody)
              .setLocalizedEmailBodyText(
                  preExistingStatus
                      .localizedEmailBodyText()
                      .orElse(LocalizedStrings.withDefaultValue(emailBody)))
              .build());
    } else {
      statusesForUpdate.add(
          StatusDefinitions.Status.builder()
              .setStatusText(statusText)
              .setLocalizedStatusText(LocalizedStrings.withDefaultValue(statusText))
              .setEmailBodyText(emailBody)
              .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue(emailBody))
              .build());
    }
    current.setStatuses(ImmutableList.copyOf(statusesForUpdate));

    return Pair.of(validatedForm, Optional.of(current));
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
