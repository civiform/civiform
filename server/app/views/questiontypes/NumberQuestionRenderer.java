package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import java.util.OptionalLong;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NumberQuestion;
import views.components.FieldWithLabel;

/** Renders a number question. */
public class NumberQuestionRenderer extends ApplicantQuestionRendererImpl {

  public NumberQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-number";
  }

  @Override
  protected Tag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    NumberQuestion numberQuestion = question.createNumberQuestion();

    FieldWithLabel numberField =
        FieldWithLabel.number()
            .setFieldName(numberQuestion.getNumberPath().toString())
            .setScreenReaderText(question.getQuestionText())
            .setMin(numberQuestion.getQuestionDefinition().getMin())
            .setMax(numberQuestion.getQuestionDefinition().getMax())
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(numberQuestion.getNumberPath(), ImmutableSet.of()))
            .addReferenceClass(getReferenceClass());
    if (numberQuestion.getNumberValue().isPresent()) {
      // TODO: [Refactor] Oof! Converting Optional<Long> to OptionalLong.
      OptionalLong value = OptionalLong.of(numberQuestion.getNumberValue().orElse(0L));
      numberField.setValue(value);
    }
    // TODO(#1944): Consider updating FieldWithLabel to allow setting a string
    // value for a number type so invalid input can be round-tripped back to the
    // user when rendering a form with errors.

    return div().with(numberField.getContainer());
  }
}
