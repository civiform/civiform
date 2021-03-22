package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;

import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;

public class NumberQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public NumberQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ApplicantQuestion.NumberQuestion numberQuestion = question.getNumberQuestion();

    return label(
        text(question.getQuestionText()),
        input()
            .withType("number")
            .withCondValue(
                numberQuestion.hasValue(),
                numberQuestion.getNumberValue().map(String::valueOf).orElse(""))
            .withName(numberQuestion.getNumberPath().path()),
        fieldErrors(numberQuestion.getQuestionErrors()));
  }
}
