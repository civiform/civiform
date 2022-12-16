package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import views.BaseHtmlView;
import views.components.TextFormatter;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;

/**
 * Superclass for all applicant question renderers with input field(s) for the applicant to answer
 * the question. Question renderers should not subclass from ApplicantQuestionRendererImpl directly;
 * instead they should subclass from one of the child classes, either
 * ApplicantCompositeQuestionRenderer (for multiple input fields) or ApplicantSingleQuestionRenderer
 * (for single input field).
 */
abstract class ApplicantQuestionRendererImpl implements ApplicantQuestionRenderer {

  protected final ApplicantQuestion question;
  // HTML id tags for various elements within this question.
  private final String questionId;
  private final String descriptionId;
  private final String errorId;

  ApplicantQuestionRendererImpl(ApplicantQuestion question) {
    this.question = checkNotNull(question);
    this.questionId = RandomStringUtils.randomAlphabetic(8);
    this.descriptionId = String.format("%s-description", questionId);
    this.errorId = String.format("%s-error", questionId);
  }

  private String getRequiredClass() {
    return question.isOptional() ? "" : ReferenceClasses.REQUIRED_QUESTION;
  }

  /** Renders the question tag. */
  protected abstract ContainerTag renderQuestionTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      ImmutableList<DomContent> questionTextDoms,
      DivTag questionSecondaryTextDiv);

  @Override
  public final DivTag render(ApplicantQuestionRendererParams params) {
    ImmutableList.Builder<String> ariaDescribedByBuilder =
        ImmutableList.<String>builder().add(descriptionId);
    Messages messages = params.messages();
    DivTag questionSecondaryTextDiv =
        div()
            .with(
                div()
                    // Question help text
                    .withId(descriptionId)
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .with(
                        TextFormatter.createLinksAndEscapeText(
                            question.getQuestionHelpText(),
                            TextFormatter.UrlOpenAction.NewTab,
                            /*addRequiredIndicator= */ false)))
            .withClasses("mb-4");

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
                  messages, questionErrors, ReferenceClasses.APPLICANT_QUESTION_ERRORS)
              .withId(errorId));
      ariaDescribedByBuilder.add(errorId);
    }

    ImmutableList<DomContent> questionTextDoms =
        TextFormatter.createLinksAndEscapeText(
            question.getQuestionText(),
            TextFormatter.UrlOpenAction.NewTab,
            /*addRequiredIndicator= */ !question.isOptional());
    // Reverse the list to have errors appear first.
    ImmutableList<String> ariaDescribedByIds = ariaDescribedByBuilder.build().reverse();

    ContainerTag questionTag =
        renderQuestionTag(
            params,
            validationErrors,
            ariaDescribedByIds,
            questionTextDoms,
            questionSecondaryTextDiv);

    return div()
        .withId(questionId)
        .withClasses("mx-auto", "mb-8", getReferenceClass(), getRequiredClass())
        .with(questionTag);
  }
}
