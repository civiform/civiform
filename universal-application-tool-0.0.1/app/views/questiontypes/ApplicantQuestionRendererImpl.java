package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableSet;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import views.BaseHtmlView;
import views.components.TextFormatter;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Superclass for all applicant question renderers with input field(s) for
 * the applicant to answer the question.
 */
public abstract class ApplicantQuestionRendererImpl implements ApplicantQuestionRenderer {

  protected final ApplicantQuestion question;

  public ApplicantQuestionRendererImpl(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  private String getRequiredClass() {
    return question.isOptional() ? "" : ReferenceClasses.REQUIRED_QUESTION;
  }

  protected abstract Tag renderTag(ApplicantQuestionRendererParams params);

  @Override
  public final Tag render(ApplicantQuestionRendererParams params) {
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
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .with(TextFormatter.createLinksAndEscapeText(question.getQuestionHelpText())))
            .withClasses(Styles.MB_4);

    ImmutableSet<ValidationErrorMessage> questionErrors = question.errorsPresenter().getQuestionErrors();
    if (!questionErrors.isEmpty()) {
      // Question error text
      questionTextDiv.with(BaseHtmlView.fieldErrors(messages, questionErrors));
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
        .with(renderTag(params));
  }
}
