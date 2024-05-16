package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import services.MessageKey;
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

  protected final ApplicantQuestion applicantQuestion;
  // HTML id tags for various elements within this question.
  private final String questionId;
  private final String descriptionId;
  private final String errorId;

  ApplicantQuestionRendererImpl(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = checkNotNull(applicantQuestion);
    this.questionId = RandomStringUtils.randomAlphabetic(8);
    this.descriptionId = String.format("%s-description", questionId);
    this.errorId = String.format("%s-error", questionId);
  }

  private String getRequiredClass() {
    return applicantQuestion.isOptional() ? "" : ReferenceClasses.REQUIRED_QUESTION;
  }

  /** Renders the question tag. */
  protected abstract ContainerTag renderQuestionTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      ImmutableList<DomContent> questionTextDoms,
      DivTag questionSecondaryTextDiv,
      boolean isOptional);

  @Override
  public final DivTag render(ApplicantQuestionRendererParams params) {
    ImmutableList.Builder<String> ariaDescribedByBuilder =
        ImmutableList.<String>builder().add(descriptionId);
    Messages messages = params.messages();
    DivTag questionSecondaryTextDiv =
        div().condWith(!applicantQuestion.getQuestionHelpText().isEmpty(), div()
            .with(
                div()
                    // Question help text
                    .withId(descriptionId)
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                        ApplicantStyles.QUESTION_HELP_TEXT)
                    .with(
                        TextFormatter.formatTextWithAriaLabel(
                            applicantQuestion.getQuestionHelpText(),
                            /* preserveEmptyLines= */ true,
                            /* addRequiredIndicator= */ false,
                            messages
                                .at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName())
                                .toLowerCase(Locale.ROOT))))
            .withClasses("mb-4"));

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors;
    if (ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrors(
        params.errorDisplayMode())) {
      validationErrors = applicantQuestion.getQuestion().getValidationErrors();
    } else {
      validationErrors = ImmutableMap.of();
    }

    ImmutableSet<ValidationErrorMessage> questionErrors =
        validationErrors.getOrDefault(applicantQuestion.getContextualizedPath(), ImmutableSet.of());
    if (!questionErrors.isEmpty()) {
      // Question error text
      questionSecondaryTextDiv.with(
          BaseHtmlView.fieldErrors(
                  messages, questionErrors, ReferenceClasses.APPLICANT_QUESTION_ERRORS)
              .withId(errorId));
      ariaDescribedByBuilder.add(errorId);
    }

    ImmutableList<DomContent> questionTextDoms =
        TextFormatter.formatTextWithAriaLabel(
            applicantQuestion.getQuestionText(),
            /* preserveEmptyLines= */ true,
            /* addRequiredIndicator= */ !applicantQuestion.isOptional(),
            messages.at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName()).toLowerCase(Locale.ROOT));
    // Reverse the list to have errors appear first.
    ImmutableList<String> ariaDescribedByIds = ariaDescribedByBuilder.build().reverse();

    ContainerTag questionTag =
        renderQuestionTag(
            params,
            validationErrors,
            ariaDescribedByIds,
            questionTextDoms,
            questionSecondaryTextDiv,
            applicantQuestion.isOptional());

    return div()
        .withId(questionId)
        .withClasses("mx-auto", "mb-8", getReferenceClass(), getRequiredClass())
        .with(questionTag);
  }
}
