package views.admin.questions;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.label;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import forms.AddressQuestionForm;
import forms.MultiOptionQuestionForm;
import forms.NumberQuestionForm;
import forms.QuestionForm;
import forms.TextQuestionForm;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;
import services.question.types.QuestionType;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class QuestionConfig {
  private String id = "";
  private String headerText = "Question Settings";
  private ContainerTag content = div();

  private static final String HEADER_CLASSES =
      StyleUtils.joinStyles(
          Styles.BG_TRANSPARENT,
          Styles.TEXT_GRAY_600,
          Styles.BLOCK,
          Styles.FONT_BOLD,
          Styles.TEXT_SM,
          Styles._MT_1,
          Styles.PB_0,
          Styles.MB_0,
          Styles.MX_2,
          Styles.UPPERCASE);

  private static final String INNER_DIV_CLASSES =
      StyleUtils.joinStyles(
          Styles.BORDER, Styles.BG_GRAY_100,
          Styles.PT_4, Styles.M_4);

  private static final String OUTER_DIV_CLASSES =
      StyleUtils.joinStyles(Styles.W_FULL, Styles.PT_0, Styles._MT_4);

  public QuestionConfig() {}

  public QuestionConfig setId(String id) {
    this.id = id;
    return this;
  }

  public QuestionConfig setHeaderText(String headerText) {
    this.headerText = headerText;
    return this;
  }

  // TODO(https://github.com/seattle-uat/civiform/issues/589): Remove QuestionType parameter once we
  //  implement the other question forms since that info will be within the question form.
  public static ContainerTag buildQuestionConfig(QuestionType type, QuestionForm questionForm) {
    QuestionConfig config = new QuestionConfig();
    // TODO(https://github.com/seattle-uat/civiform/issues/589): Switch on type of question form
    //  once we implement other question forms. May also help us avoid casting the question form.
    switch (type) {
      case TEXT:
        return config
            .setId("text-question-config")
            .addTextQuestionConfig((TextQuestionForm) questionForm)
            .getContainer();
      case ADDRESS:
        return config
            .setId("address-question-config")
            .addAddressQuestionConfig((AddressQuestionForm) questionForm)
            .getContainer();
      case CHECKBOX:
        MultiOptionQuestionForm form = (MultiOptionQuestionForm) questionForm;
        return config
            .setId("multi-select-question-config")
            .addMultiOptionQuestionFields(form)
            .addMultiSelectQuestionValidation(form)
            .getContainer();
      case NUMBER:
        return config
                .setId("number-question-config")
                .addNumberQuestionConfig((NumberQuestionForm) questionForm)
                .getContainer();
      case RADIO_BUTTON:
        return config
            .setId("single-select-question-config")
            .addMultiOptionQuestionFields((MultiOptionQuestionForm) questionForm)
            .getContainer();
      case DROPDOWN: // fallthrough intended - no options
      case NAME: // fallthrough intended - no options
      case REPEATER: // fallthrough intended
      default:
        return div();
    }
  }

  private QuestionConfig addAddressQuestionConfig(AddressQuestionForm addressQuestionForm) {
    content.with(
        new SelectWithLabel()
            .setId("address-question-default-state-select")
            .setFieldName("defaultState")
            .setLabelText("Default State")
            .setOptions(stateOptions())
            .setValue("-")
            .getContainer(),
        FieldWithLabel.checkbox()
            .setId("address-question-disallow-po-box-checkbox")
            .setFieldName("disallowPoBox")
            .setLabelText("Disallow post office boxes")
            .setChecked(addressQuestionForm.getDisallowPoBox())
            .getContainer(),
        FieldWithLabel.checkbox()
            .setId("address-question-include-none-checkbox")
            .setFieldName("noAddress")
            .setLabelText("Include \"No address\" option")
            .getContainer());
    return this;
  }

  private QuestionConfig addTextQuestionConfig(TextQuestionForm textQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setId("text-question-min-length-input")
            .setFieldName("minLength")
            .setLabelText("Min length")
            .setValue(textQuestionForm.getMinLength())
            .getContainer(),
        FieldWithLabel.number()
            .setId("text-question-max-length-input")
            .setFieldName("maxLength")
            .setLabelText("Maximum length")
            .setValue(textQuestionForm.getMaxLength())
            .getContainer());
    return this;
  }

  /**
   * Creates an individual text field where an admin can enter a single multi-option question
   * answer, along with a button to remove the option.
   */
  public static ContainerTag multiOptionQuestionField(Optional<String> existingOption) {
    ContainerTag optionInput =
        FieldWithLabel.input()
            .setFieldName("options[]")
            .setLabelText("Question option")
            .setValue(existingOption)
            .getContainer()
            .withClasses(Styles.FLEX, Styles.ML_2);
    Tag removeOptionButton =
        button("Remove").withType("button").withClasses(Styles.FLEX, Styles.ML_4);

    return div()
        .withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.MB_4)
        .with(optionInput, removeOptionButton);
  }

  private QuestionConfig addMultiOptionQuestionFields(
      MultiOptionQuestionForm multiOptionQuestionForm) {
    ImmutableList<ContainerTag> existingOptions =
        multiOptionQuestionForm.getOptions().stream()
            .map(option -> multiOptionQuestionField(Optional.of(option)))
            .collect(toImmutableList());

    content
        .with(existingOptions)
        .with(
            button("Add answer option")
                .withType("button")
                .withId("add-new-option")
                .withClasses(Styles.M_2));
    return this;
  }

  /**
   * Creates two number input fields, where an admin can specify the min and max number of choices
   * allowed for multi-select questions.
   */
  private QuestionConfig addMultiSelectQuestionValidation(MultiOptionQuestionForm multiOptionForm) {
    content.with(
        FieldWithLabel.number()
            .setId("multi-select-min-choices-input")
            .setFieldName("minChoicesRequired")
            .setLabelText("Minimum number of choices required")
            .setValue(multiOptionForm.getMinChoicesRequired())
            .getContainer(),
        FieldWithLabel.number()
            .setId("multi-select-max-choices-input")
            .setFieldName("maxChoicesAllowed")
            .setLabelText("Maximum number of choices allowed")
            .setValue(multiOptionForm.getMaxChoicesAllowed())
            .getContainer());
    return this;
  }

  private QuestionConfig addNumberQuestionConfig(NumberQuestionForm numberQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setId("number-question-min-value-input")
            .setFieldName("min")
            .setLabelText("Minimum value")
            .setValue(numberQuestionForm.getMin())
            .getContainer(),
        FieldWithLabel.number()
            .setId("number-question-max-value-input")
            .setFieldName("max")
            .setLabelText("Maximum value")
            .setValue(numberQuestionForm.getMax())
            .getContainer());
    return this;
  }

  public ContainerTag getContainer() {
    return div()
        .withCondId(!Strings.isNullOrEmpty(this.id), this.id)
        .withClasses(ReferenceClasses.QUESTION_CONFIG)
        .with(headerLabel(this.headerText))
        .with(
            div()
                .withClasses(OUTER_DIV_CLASSES)
                .with(content.withId("question-settings").withClasses(INNER_DIV_CLASSES)));
  }

  private static ContainerTag headerLabel(String text) {
    return label().withClasses(HEADER_CLASSES).withText(text);
  }

  /**
   * I don't feel like hard-coding a list of states here, so this will do until we can think up a
   * better approach.
   */
  private static ImmutableList<SimpleEntry<String, String>> stateOptions() {
    return ImmutableList.of(
        new SimpleEntry<>("-- Leave blank --", "-"), new SimpleEntry<>("Washington", "WA"));
  }
}
