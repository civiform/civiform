package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import views.BaseHtmlView;
import views.components.TextFormatter;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Superclass for all applicant question renderers. An applicant question renderer renders a
 * question to be seen by an applicant, including the input field(s) for the applicant to answer the
 * question.
 */
public abstract class ApplicantQuestionRenderer {

  final ApplicantQuestion question;

  public ApplicantQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  public abstract String getReferenceClass();

  public abstract Tag render(ApplicantQuestionRendererParams params);

  private String getRequiredClass() {
    return question.isOptional() ? "" : ReferenceClasses.REQUIRED_QUESTION;
  }

  protected Tag renderInternal(Messages messages, Tag questionFormContent) {
    return renderInternal(messages, questionFormContent, true);
  }

  /**
   * Returns true if the question is required but skipped in the current program (i.e. the applicant
   * clicked "next" without answering the question) or if the question's contents does not pass
   * question-specific validation checks.
   */
  public boolean isInvalid() {
    return question.isRequiredButWasSkippedInCurrentProgram()
        || !question.getQuestionErrors().isEmpty();
  }

  protected String questionHtmlId() {
    return question.getContextualizedPath().toString();
  }

  protected String questionErrorMessageHtmlId() {
    return questionHtmlId() + "-error";
  }

  protected String questionHelpTextHtmlId() {
    return questionHtmlId() + "-help-text";
  }

  /**
   * Renders an applicant question's text, help text, errors, and the given form content.
   *
   * <p>In some cases, like text questions and number questions, the question errors are rendered in
   * the form content, so we offer the ability to specify whether or not this method should render
   * the question errors here.
   */
  protected Tag renderInternal(
      Messages messages, Tag questionFormContent, boolean shouldDisplayQuestionErrors) {

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
                    .withId(questionHelpTextHtmlId())
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .with(TextFormatter.createLinksAndEscapeText(question.getQuestionHelpText())))
            .withClasses(Styles.MB_4);

    ContainerTag errorsDiv = div().withId(questionErrorMessageHtmlId());

    if (shouldDisplayQuestionErrors) {
      // Question error text
      errorsDiv.with(BaseHtmlView.fieldErrors(messages, question.getQuestionErrors()));
    }

    if (question.isRequiredButWasSkippedInCurrentProgram()) {
      String requiredQuestionMessage = messages.at(MessageKey.VALIDATION_REQUIRED.getKeyName());
      errorsDiv.with(
          div()
              .withId(questionErrorMessageHtmlId())
              .withClasses(Styles.P_1, Styles.TEXT_RED_600)
              .withText("*" + requiredQuestionMessage));
    }

    return div()
        .withId(questionHtmlId())
        .withClasses(Styles.MX_AUTO, Styles.MB_8, getReferenceClass(), getRequiredClass())
        .with(questionTextDiv.with(errorsDiv))
        .with(questionFormContent);
  }
}
