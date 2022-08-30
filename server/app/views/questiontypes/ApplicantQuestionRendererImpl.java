package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.legend;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
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

  /** Whether the question is comprised of a single input field or multiple. */
  protected enum InputFieldType {
    SINGLE,
    COMPOSITE
  }

  protected final ApplicantQuestion question;
  private final InputFieldType inputFieldType;

  ApplicantQuestionRendererImpl(ApplicantQuestion question, InputFieldType inputFieldType) {
    this.question = checkNotNull(question);
    this.inputFieldType = checkNotNull(inputFieldType);
  }

  private String getRequiredClass() {
    return question.isOptional() ? "" : ReferenceClasses.REQUIRED_QUESTION;
  }

  protected abstract DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors);

  @Override
  public final DivTag render(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    DivTag questionSecondaryTextDiv =
        div()
            .with(
                div()
                    // Question help text
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .with(
                        TextFormatter.createLinksAndEscapeText(
                            question.getQuestionHelpText(), TextFormatter.UrlOpenAction.NewTab)))
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

    ImmutableSet<ValidationErrorMessage> questionErrors =
        validationErrors.getOrDefault(question.getContextualizedPath(), ImmutableSet.of());
    if (!questionErrors.isEmpty()) {
      // Question error text
      questionSecondaryTextDiv.with(
          BaseHtmlView.fieldErrors(
              messages, questionErrors, ReferenceClasses.APPLICANT_QUESTION_ERRORS));
    }

    if (question.isRequiredButWasSkippedInCurrentProgram()) {
      String requiredQuestionMessage = messages.at(MessageKey.VALIDATION_REQUIRED.getKeyName());
      questionSecondaryTextDiv.with(
          div()
              .withClasses(Styles.P_1, Styles.TEXT_RED_600)
              .withText("*" + requiredQuestionMessage));
    }

    ContainerTag questionTag;
    ImmutableList<DomContent> questionTextDoms =
        TextFormatter.createLinksAndEscapeText(
            question.getQuestionText(), TextFormatter.UrlOpenAction.NewTab);
    switch (inputFieldType) {
      case COMPOSITE:
        // Composite fields should be rendered with fieldset and legend for screen reader users.
        questionTag =
            fieldset()
                .with(
                    // Legend must be a direct child of fieldset for screen readers to work
                    // properly.
                    legend()
                        .with(questionTextDoms)
                        .withClasses(
                            ReferenceClasses.APPLICANT_QUESTION_TEXT,
                            ApplicantStyles.QUESTION_TEXT))
                .with(questionSecondaryTextDiv)
                .with(renderTag(params, validationErrors));
        break;
      case SINGLE:
        questionTag =
            div()
                .with(
                    div()
                        .with(questionTextDoms)
                        .withClasses(
                            ReferenceClasses.APPLICANT_QUESTION_TEXT,
                            ApplicantStyles.QUESTION_TEXT))
                .with(questionSecondaryTextDiv)
                .with(renderTag(params, validationErrors));
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Unhandled input field type: %s", inputFieldType));
    }

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.MB_8, getReferenceClass(), getRequiredClass())
        .with(questionTag);
  }
}
