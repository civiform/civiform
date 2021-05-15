package views.questiontypes;

import static j2html.TagCreator.div;

import j2html.tags.Tag;
import java.util.OptionalLong;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NumberQuestion;
import views.components.FieldWithLabel;

public class NumberQuestionRenderer extends ApplicantQuestionRenderer {

  public NumberQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    NumberQuestion numberQuestion = question.createNumberQuestion();

    FieldWithLabel numberField =
        FieldWithLabel.number()
            .setFieldName(numberQuestion.getNumberPath().toString())
            .setFieldErrors(params.messages(), numberQuestion.getQuestionErrors());
    if (numberQuestion.getNumberValue().isPresent()) {
      // TODO: [Refactor] Oof! Converting Optional<Long> to OptionalLong.
      OptionalLong value = OptionalLong.of(numberQuestion.getNumberValue().orElse(0L));
      numberField.setValue(value);
    }

    Tag numberQuestionFormContent = div().with(numberField.getContainer());

    return renderInternal(params.messages(), numberQuestionFormContent, false);
  }
}
