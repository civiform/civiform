package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;

import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;

public class TextQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public TextQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ApplicantQuestion.TextQuestion textQuestion = question.getTextQuestion();

    return label(
        text(question.getQuestionText()),
        input().withType("text")
            .withCondValue(textQuestion.hasValue(), textQuestion.getTextValue().orElse(""))
        .withName(textQuestion.getTextPath())
    );
  }
}
