package views.questiontypes;

import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.legend;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import org.apache.commons.lang3.StringUtils;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;

/*
 * Superclass for questions that have multiple input fields.
 *
 * Questions with multiple input fields should use a fieldset and legend for a11y. Question level
 * errors and descriptions are attached at the fieldset level, instead of to individual inputs.
 */
abstract class ApplicantCompositeQuestionRenderer extends ApplicantQuestionRendererImpl {

  ApplicantCompositeQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  protected abstract DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors);

  @Override
  protected final ContainerTag renderQuestionTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      ImmutableList<DomContent> questionTextDoms,
      DivTag questionSecondaryTextDiv) {
    ContainerTag questionTag =
        fieldset()
            .attr("aria-describedby", StringUtils.join(ariaDescribedByIds, " "))
            .with(
                // Legend must be a direct child of fieldset for screen readers to work
                // properly.
                legend()
                    .with(questionTextDoms)
                    .withClasses(
                        ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT))
            .with(questionSecondaryTextDiv)
            .with(renderInputTags(params, validationErrors));

    return questionTag;
  }
}
