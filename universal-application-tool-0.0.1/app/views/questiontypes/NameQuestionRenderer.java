package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;

import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;

public class NameQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public NameQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ApplicantQuestion.NameQuestion nameQuestion = question.getNameQuestion();

    return div(
        text(question.getQuestionText()),
        label(
            input()
                .withType("text")
                .withCondValue(
                    nameQuestion.hasFirstNameValue(), nameQuestion.getFirstNameValue().orElse(""))
                .withName(nameQuestion.getFirstNamePath()),
            renderFieldErrors(nameQuestion.getFirstNameErrors())),
        label(
            input()
                .withType("text")
                .withCondValue(
                    nameQuestion.hasMiddleNameValue(), nameQuestion.getMiddleNameValue().orElse(""))
                .withName(nameQuestion.getMiddleNamePath())),
        label(
            input()
                .withType("text")
                .withCondValue(
                    nameQuestion.hasLastNameValue(), nameQuestion.getLastNameValue().orElse(""))
                .withName(nameQuestion.getLastNamePath()),
            renderFieldErrors(nameQuestion.getLastNameErrors())));
  }
}
