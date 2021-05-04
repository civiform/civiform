package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import j2html.attributes.Attr;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class DropdownQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public DropdownQuestionRenderer(ApplicantQuestion question) {
    this.question = question;
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    SingleSelectQuestion singleSelectQuestion = question.createSingleSelectQuestion();

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.W_MAX)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT)
                .withText(question.getQuestionText()),
            div()
                .withClasses(
                    ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                    Styles.TEXT_BASE,
                    Styles.FONT_THIN,
                    Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            select()
                .withName(singleSelectQuestion.getSelectionPath().toString())
                .with(
                    each(
                        singleSelectQuestion.getOptions(),
                        option ->
                            option(option.optionText())
                                .withValue(String.valueOf(option.id()))
                                .condAttr(
                                    singleSelectQuestion.optionIsSelected(option),
                                    Attr.SELECTED,
                                    "selected"))),
            fieldErrors(params.messages(), singleSelectQuestion.getQuestionErrors())
                .withClasses(Styles.ML_2, Styles.TEXT_XS, Styles.TEXT_RED_600, Styles.FONT_BOLD));
  }
}
