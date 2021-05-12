package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import j2html.tags.Tag;
import java.util.AbstractMap;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import views.BaseHtmlView;
import views.components.SelectWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

public class DropdownQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public DropdownQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    SingleSelectQuestion singleSelectQuestion = question.createSingleSelectQuestion();

    SelectWithLabel select =
        new SelectWithLabel()
            .setFieldName(singleSelectQuestion.getSelectionPath().toString())
            .setOptions(
                singleSelectQuestion.getOptions().stream()
                    .map(
                        option ->
                            new AbstractMap.SimpleEntry<>(
                                option.optionText(), String.valueOf(option.id())))
                    .collect(toImmutableList()));

    if (singleSelectQuestion.getSelectedOptionId().isPresent()) {
      select.setValue(String.valueOf(singleSelectQuestion.getSelectedOptionId().get()));
    }

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
            select.getContainer(),
            fieldErrors(params.messages(), singleSelectQuestion.getQuestionErrors())
                .withClasses(Styles.ML_2, Styles.TEXT_XS, Styles.TEXT_RED_600, Styles.FONT_BOLD));
  }
}
