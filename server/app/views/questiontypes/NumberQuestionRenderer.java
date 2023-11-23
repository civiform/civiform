package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.util.OptionalLong;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NumberQuestion;
import views.components.FieldWithLabel;

/** Renders a number question. */
public class NumberQuestionRenderer extends ApplicantSingleQuestionRenderer {

  public NumberQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-number";
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {
    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    FieldWithLabel numberField =
        FieldWithLabel.number()
            .setFieldName(numberQuestion.getNumberPath().toString())
            .setScreenReaderText(applicantQuestion.getQuestionTextForScreenReader())
            .setMin(numberQuestion.getQuestionDefinition().getMin())
            .setMax(numberQuestion.getQuestionDefinition().getMax())
            .setAriaRequired(!isOptional)
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(numberQuestion.getNumberPath(), ImmutableSet.of()))
            .setAriaDescribedByIds(ariaDescribedByIds)
            .addReferenceClass(getReferenceClass());

    if (params.autofocusSingleField()) {
      numberField.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      numberField.forceAriaInvalid();
    }

    if (numberQuestion.getNumberValue().isPresent()) {
      // Note: If the provided input was invalid, there's no use rendering
      // the value on roundtrip since inputs with type="number" won't allow
      // setting a value that doesn't conform to the expected format.
      // TODO: [Refactor] Oof! Converting Optional<Long> to OptionalLong.
      OptionalLong value = OptionalLong.of(numberQuestion.getNumberValue().orElse(0L));
      numberField.setValue(value);
    }

    return div().with(numberField.getNumberTag());
  }
}
