package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.IdQuestion;
import views.components.FieldWithLabel;

/** Renders an id question. */
public class IdQuestionRenderer extends ApplicantQuestionRendererImpl {

  public IdQuestionRenderer(ApplicantQuestion question) {
    super(question, InputFieldType.SINGLE);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-id";
  }

  @Override
  protected DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean hasErrors) {
    IdQuestion idQuestion = question.createIdQuestion();

    DivTag questionFormContent =
        FieldWithLabel.input()
            .setFieldName(idQuestion.getIdPath().toString())
            .setValue(idQuestion.getIdValue().orElse(""))
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(idQuestion.getIdPath(), ImmutableSet.of()))
            .setAriaInvalid(hasErrors)
            .setAriaDescribedByIds(ariaDescribedByIds)
            .setScreenReaderText(question.getQuestionText())
            .getInputTag();

    return questionFormContent;
  }
}
