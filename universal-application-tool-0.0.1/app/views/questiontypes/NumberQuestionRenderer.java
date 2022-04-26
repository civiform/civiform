package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import java.util.OptionalLong;
import services.MessageKey;
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
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    NumberQuestion numberQuestion = question.createNumberQuestion();

    FieldWithLabel numberField =
        FieldWithLabel.number()
            .setFieldName(numberQuestion.getNumberPath().toString())
            .setScreenReaderText(question.getQuestionText())
            .setMin(numberQuestion.getQuestionDefinition().getMin())
            .setMax(numberQuestion.getQuestionDefinition().getMax())
            .setFieldErrors(
                params.messages(),
                // TODO(#1944): Replicate the client-side validation on the server-side.
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_NON_INTEGER)))
            .showFieldErrors(false)
            .addReferenceClass(getReferenceClass());
    if (numberQuestion.getNumberValue().isPresent()) {
      // TODO: [Refactor] Oof! Converting Optional<Long> to OptionalLong.
      OptionalLong value = OptionalLong.of(numberQuestion.getNumberValue().orElse(0L));
      numberField.setValue(value);
    }

    Tag numberQuestionFormContent = div().with(numberField.getContainer());

    return numberQuestionFormContent;
  }
}
