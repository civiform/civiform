package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.ArrayList;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import views.BaseHtmlView;
import views.components.TextFormatter;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Superclass for all applicant question renderers with input field(s) for the applicant to answer
 * the question.
 */
abstract class ApplicantQuestionRendererImpl implements ApplicantQuestionRenderer {

  protected final ApplicantQuestion question;

  // HTML id tags for various elements within this question.
  private final String id;
  private final String descriptionId;
  private final String requiredErrorId;
  private final String validationErrorId;

  ApplicantQuestionRendererImpl(ApplicantQuestion question) {
    this.question = checkNotNull(question);
    id = question.getContextualizedPath().toString();
    descriptionId = String.format("%s-description", id);
    requiredErrorId = String.format("%s-required-error", id);
    validationErrorId = String.format("%s-validation-error", id);
  }

  private String getRequiredClass() {
    return question.isOptional() ? "" : ReferenceClasses.REQUIRED_QUESTION;
  }

  // Add comments?
  // A list of HTML ids, used to provide question-level details for accessibility. This is currently
  // used by the "aria-describedby" attribute for each input for this question.

  protected abstract Tag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ArrayList<String> ariaDescribedByIds,
      boolean hasQuestionErrors);

  @Override
  public final Tag render(ApplicantQuestionRendererParams params) {
    var ariaDescribedByIds = new ArrayList<String>();
    ariaDescribedByIds.add(descriptionId);

    Messages messages = params.messages();
    ContainerTag questionTextDiv =
        div()
            // Question text
            .with(
                div()
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT)
                    .with(TextFormatter.createLinksAndEscapeText(question.getQuestionText())))
            // Question help text
            .with(
                div()
                    .withId(descriptionId)
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .with(TextFormatter.createLinksAndEscapeText(question.getQuestionHelpText())))
            .withClasses(Styles.MB_4);

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors;
    switch (params.errorDisplayMode()) {
      case HIDE_ERRORS:
        validationErrors = ImmutableMap.of();
        break;
      case DISPLAY_ERRORS:
        validationErrors = question.errorsPresenter().getValidationErrors();
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Unhandled error display mode: %s", params.errorDisplayMode()));
    }

    boolean hasQuestionErrors = false;
    ImmutableSet<ValidationErrorMessage> questionErrors =
        validationErrors.getOrDefault(question.getContextualizedPath(), ImmutableSet.of());
    if (!questionErrors.isEmpty()) {
      // Question error text
      // DELETE: if multiple errors, how will screen reader read this out? I think will read all out
      // based on manual test with currency example
      hasQuestionErrors = true;
      questionTextDiv.with(
          BaseHtmlView.fieldErrors(
                  messages, questionErrors, ReferenceClasses.APPLICANT_QUESTION_ERRORS)
              .withId(validationErrorId));
      // Insert error message to be read first.
      ariaDescribedByIds.add(0, validationErrorId);
    }

    if (question.isRequiredButWasSkippedInCurrentProgram()) {
      hasQuestionErrors = true;
      String requiredQuestionMessage = messages.at(MessageKey.VALIDATION_REQUIRED.getKeyName());
      questionTextDiv.with(
          div()
              .withId(requiredErrorId)
              .withClasses(Styles.P_1, Styles.TEXT_RED_600)
              .withText("*" + requiredQuestionMessage));
      // Insert error message to be read first.
      ariaDescribedByIds.add(0, requiredErrorId);
    }

    return div()
        .withId(id)
        .withClasses(Styles.MX_AUTO, Styles.MB_8, getReferenceClass(), getRequiredClass())
        .with(questionTextDiv)
        .with(renderTag(params, validationErrors, ariaDescribedByIds, hasQuestionErrors));

    // DELETE - what's the cleanest way to pass this data in? (hasQuestionErrors and question level
    // aria-describedby)
    // Add wrapper struct? Refactor it to make it a property of the class?
  }
}
