package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


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

import j2html.tags.specialized.DivTag;

/**
 * Superclass for all applicant question renderers with input field(s) for the applicant to answer
 * the question.
 */
abstract class ApplicantQuestionRendererImpl implements ApplicantQuestionRenderer {

  protected final ApplicantQuestion question;

  ApplicantQuestionRendererImpl(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  private String getRequiredClass() {
    return question.isOptional() ? "" : ReferenceClasses.REQUIRED_QUESTION;
  }

  protected abstract Tag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors);

  @Override
  public final DivTag render(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    DivTag questionTextDiv =
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

    ImmutableSet<ValidationErrorMessage> questionErrors =
        validationErrors.getOrDefault(question.getContextualizedPath(), ImmutableSet.of());
    if (!questionErrors.isEmpty()) {
      // Question error text
      questionTextDiv.with(
          BaseHtmlView.fieldErrors(
              messages, questionErrors, ReferenceClasses.APPLICANT_QUESTION_ERRORS));
    }

    if (question.isRequiredButWasSkippedInCurrentProgram()) {
      String requiredQuestionMessage = messages.at(MessageKey.VALIDATION_REQUIRED.getKeyName());
      questionTextDiv.with(
          div()
              .withClasses(Styles.P_1, Styles.TEXT_RED_600)
              .withText("*" + requiredQuestionMessage));
    }

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.MB_8, getReferenceClass(), getRequiredClass())
        .with(questionTextDiv)
        .with(renderTag(params, validationErrors));
  }
}
