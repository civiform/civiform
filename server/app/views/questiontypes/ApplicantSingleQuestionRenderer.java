package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;

/*
 * Superclass for questions that have a single input field.
 */
abstract class ApplicantSingleQuestionRenderer extends ApplicantQuestionRendererImpl {

  ApplicantSingleQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  protected abstract DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds);

  @Override
  protected final ContainerTag renderQuestionTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      ImmutableList<DomContent> questionTextDoms,
      DivTag questionSecondaryTextDiv) {
    ContainerTag questionTag =
        div()
            .with(
                div()
                    .with(questionTextDoms)
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT))
            .with(questionSecondaryTextDiv)
            .with(renderInputTag(params, validationErrors, ariaDescribedByIds));

    return questionTag;
  }
}
