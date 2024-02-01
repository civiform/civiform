package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import org.apache.commons.lang3.StringUtils;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.IdQuestion;
import views.components.FieldWithLabel;

/** Renders an id question. */
public class IdQuestionRenderer extends ApplicantSingleQuestionRenderer {

  public IdQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-id";
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {

    IdQuestion idQuestion = applicantQuestion.createIdQuestion();

    ImmutableList<String> errors =
        validationErrors.getOrDefault(idQuestion.getIdPath(), ImmutableSet.of()).stream()
            .map((ValidationErrorMessage vem) -> vem.getMessage(params.messages()))
            .collect(ImmutableList.toImmutableList());
    FieldWithLabel idField = FieldWithLabel.input();
    DivTag thymeleafContent =
        div()
            .attr("hx-swap", "outerHTML")
            .attr(
                "hx-get",
                controllers.applicant.routes.NorthStarQuestionController.idQuestion(
                    idField.getId(),
                    idQuestion.getIdPath().toString(),
                    StringUtils.join(ariaDescribedByIds, " "),
                    applicantQuestion.getQuestionTextForScreenReader(),
                    idQuestion.getIdValue().orElse(""),
                    !isOptional,
                    params.autofocusSingleField(),
                    errors))
            .attr("hx-trigger", "load");
    return thymeleafContent;
  }
}
