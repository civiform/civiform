package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

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

  Tag renderInternal(Messages messages, Tag questionFormContent) {
    return renderInternal(messages, questionFormContent, true);
  }

  /**
   * Renders an applicant question's text, help text, errors, and the given form content.
   *
   * <p>In some cases, like text questions and number questions, the question errors are rendered in
   * the form content, so we offer the ability to specify whether or not this method should render
   * the question errors here.
   */
  Tag renderInternal(
      Messages messages, Tag questionFormContent, boolean shouldDisplayQuestionErrors) {

    ContainerTag questionTextDiv =
        div()
            // Question text
            .with(createQuestionTextTag(messages))
            // Question help text
            .with(
                div()
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .with(TextFormatter.createLinksAndEscapeText(question.getQuestionHelpText())))
            .withClasses(Styles.MB_4);

    if (shouldDisplayQuestionErrors) {
      // Question error text
      questionTextDiv.with(BaseHtmlView.fieldErrors(messages, question.getQuestionErrors()));
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
        .with(questionFormContent);
  }

  private ContainerTag createQuestionTextTag(Messages messages) {
    ContainerTag containerTag =
        div()
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT)
            .with(TextFormatter.createLinksAndEscapeText(question.getQuestionText()));

    if (!question.isOptional()) {
      containerTag.with(span("*").withClasses(Styles.P_1, Styles.TEXT_RED_600));
    } else {
      containerTag.with(
          span(messages.at(MessageKey.CONTENT_OPTIONAL_QUESTION_TEXT.getKeyName()))
              .withClass(Styles.P_1));
    }

    return containerTag;
  }
}
