package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.i18n.Messages;
import services.applicant.question.ApplicantQuestion;
import views.BaseHtmlView;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

public abstract class ApplicantQuestionRenderer {

  final ApplicantQuestion question;

  public ApplicantQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  public abstract String getQuestionType();
  
  public abstract Tag render(ApplicantQuestionRendererParams params);

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
            .with(
                div()
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT)
                    .withText(question.getQuestionText()))
            // Question help text
            .with(
                div()
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .withText(question.getQuestionHelpText()))
            .withClasses(Styles.MB_4);

    if (shouldDisplayQuestionErrors) {
      // Question error text
      questionTextDiv.with(BaseHtmlView.fieldErrors(messages, question.getQuestionErrors()));
    }

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.MB_8, this.getQuestionType())
        .with(questionTextDiv)
        .with(questionFormContent);
  }
}
