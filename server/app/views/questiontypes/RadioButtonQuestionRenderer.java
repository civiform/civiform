package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.InputTag;
import java.util.Comparator;
import org.apache.commons.lang3.RandomStringUtils;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a radio button question. */
public class RadioButtonQuestionRenderer extends ApplicantQuestionRendererImpl {

  public RadioButtonQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-radio";
  }

  @Override
  protected DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    SingleSelectQuestion singleOptionQuestion = question.createSingleSelectQuestion();

    DivTag radioQuestionFormContent =
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

    return radioQuestionFormContent;
  }

  private DivTag renderRadioOption(
      String selectionPath, LocalizedQuestionOption option, boolean checked) {
    String id = RandomStringUtils.randomAlphabetic(8);

    LabelTag labelTag =
        label()
            .withFor(id)
            .withText(option.optionText());
    InputTag inputTag = 
        input()
            .withId(id)
            .withType("radio")
            .withName(selectionPath)
            .withValue(String.valueOf(option.id()))
            .withCondChecked(checked)
            .withClasses(
                StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.RADIO));

    return div().withClasses(Styles.MY_2, Styles.RELATIVE, 
                  ReferenceClasses.RADIO_OPTION,
                  BaseStyles.RADIO_LABEL,
                  checked ? BaseStyles.BORDER_SEATTLE_BLUE : "")
                .with(inputTag)
                .with(labelTag);
  }
}
