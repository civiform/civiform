package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Comparator;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a radio button question. */
public class RadioButtonQuestionRenderer extends ApplicantQuestionRenderer {

  public RadioButtonQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-radio";
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    SingleSelectQuestion singleOptionQuestion = question.createSingleSelectQuestion();

    Tag radioQuestionFormContent =
        div()
            .with(
                singleOptionQuestion.getOptions().stream()
                    .sorted(Comparator.comparing(LocalizedQuestionOption::order))
                    .map(
                        option ->
                            renderRadioOption(
                                singleOptionQuestion.getSelectionPath().toString(),
                                option,
                                singleOptionQuestion.optionIsSelected(option))));

    return renderInternal(params.messages(), radioQuestionFormContent);
  }

  private Tag renderRadioOption(
      String selectionPath, LocalizedQuestionOption option, boolean checked) {
    String id = option.optionText().replaceAll("\\s+", "_");

    ContainerTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.RADIO_LABEL,
                checked ? BaseStyles.BORDER_SEATTLE_BLUE : "")
            .with(
                input()
                    .withId(id)
                    .withType("radio")
                    .withName(selectionPath)
                    .withValue(String.valueOf(option.id()))
                    .condAttr(checked, Attr.CHECKED, "")
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.RADIO)))
            .withText(option.optionText());

    return div().withClasses(Styles.MY_2, Styles.RELATIVE).with(labelTag);
  }
}
