package views.admin.questions;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.label;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import forms.AddressQuestionForm;
import forms.EnumeratorQuestionForm;
import forms.MultiOptionQuestionForm;
import forms.NumberQuestionForm;
import forms.QuestionForm;
import forms.TextQuestionForm;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import services.LocalizedStrings;
import services.question.LocalizedQuestionOption;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Contains methods for rendering type-specific question settings components. */
public class QuestionConfig {
  private String id = "";
  private String headerText = "Question settings";
  private ContainerTag content = div();

  private static final String HEADER_CLASSES =
      StyleUtils.joinStyles(
          Styles.BG_TRANSPARENT,
          Styles.TEXT_GRAY_600,
          Styles.BLOCK,
          Styles.TEXT_BASE,
          Styles._MT_1,
          Styles.PB_0,
          Styles.MB_0,
          Styles.MX_2);

  private static final String INNER_DIV_CLASSES =
      StyleUtils.joinStyles(
          Styles.BORDER, Styles.BG_GRAY_100,
          Styles.P_4, Styles.M_4);

  private static final String OUTER_DIV_CLASSES =
      StyleUtils.joinStyles(Styles.W_FULL, Styles.PT_0, Styles._MT_4);

  public QuestionConfig setId(String id) {
    this.id = id;
    return this;
  }

  public QuestionConfig setHeaderText(String headerText) {
    this.headerText = headerText;
    return this;
  }

  public static ContainerTag buildQuestionConfig(QuestionForm questionForm) {
    QuestionConfig config = new QuestionConfig();
    switch (questionForm.getQuestionType()) {
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
      case ENUMERATOR:
        return config
            .setId("enumerator-question-config")
            .addEnumeratorQuestionConfig((EnumeratorQuestionForm) questionForm)
            .getContainer();
      case NUMBER:
        return config
            .setId("number-question-config")
            .addNumberQuestionConfig((NumberQuestionForm) questionForm)
            .getContainer();
      case TEXT:
        return config
            .setId("text-question-config")
            .addTextQuestionConfig((TextQuestionForm) questionForm)
            .getContainer();
      case DROPDOWN: // fallthrough to RADIO_BUTTON
      case RADIO_BUTTON:
        return config
            .setId("single-select-question-config")
            .addMultiOptionQuestionFields((MultiOptionQuestionForm) questionForm)
            .getContainer();
      case FILEUPLOAD: // fallthrough intended
      case NAME: // fallthrough intended - no options
      case DATE: // fallthrough intended
      case EMAIL: // fallthrough intended
      default:
        return div();
    }
  }

  private QuestionConfig addAddressQuestionConfig(AddressQuestionForm addressQuestionForm) {
    content.with(
        new SelectWithLabel()
            .setId("address-question-default-state-select")
            .setFieldName("defaultState")
            .setLabelText("Default state")
            .setOptions(stateOptions())
            .setValue("-")
            .getContainer(),
        FieldWithLabel.checkbox()
            .setId("address-question-disallow-po-box-checkbox")
            .setFieldName("disallowPoBox")
            .setLabelText("Disallow post office boxes")
            .setValue("true")
            .setChecked(addressQuestionForm.getDisallowPoBox())
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

  private QuestionConfig addEnumeratorQuestionConfig(
      EnumeratorQuestionForm enumeratorQuestionForm) {
    content.with(
        FieldWithLabel.input()
            .setId("enumerator-question-entity-type-input")
            .setFieldName("entityType")
            .setLabelText("Repeated entity type")
            .setPlaceholderText("What are we enumerating?")
            .setValue(enumeratorQuestionForm.getEntityType())
            .getContainer());
    return this;
  }

  /**
   * Creates an individual text field where an admin can enter a single multi-option question
   * answer, along with a button to remove the option.
   */
  public static ContainerTag multiOptionQuestionField(
      Optional<LocalizedQuestionOption> existingOption) {
    ContainerTag optionInput =
        FieldWithLabel.input()
            .setFieldName(existingOption.isPresent() ? "options[]" : "newOptions[]")
            .setLabelText("Question option")
            .setValue(existingOption.map(LocalizedQuestionOption::optionText))
            .getContainer()
            .withClasses(Styles.FLEX, Styles.ML_2);
    ContainerTag optionIndexInput =
        existingOption.isPresent()
            ? FieldWithLabel.input()
                .setFieldName("optionIds[]")
                .setValue(String.valueOf(existingOption.get().id()))
                .setScreenReaderText("option ids")
                .getContainer()
                .withClasses(Styles.HIDDEN)
            : div();
    Tag removeOptionButton =
        button("Remove")
            .withType("button")
            .withClasses(Styles.FLEX, Styles.ML_4, "multi-option-question-field-remove-button");

    return div()
        .withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.MB_4)
        .with(optionInput, optionIndexInput, removeOptionButton);
  }

  private QuestionConfig addMultiOptionQuestionFields(
      MultiOptionQuestionForm multiOptionQuestionForm) {
    Preconditions.checkState(
        multiOptionQuestionForm.getOptionIds().size()
            == multiOptionQuestionForm.getOptions().size(),
        "Options and Option Indexes need to be the same size.");
    ImmutableList.Builder<ContainerTag> existingOptionsBuilder = ImmutableList.builder();
    for (int i = 0; i < multiOptionQuestionForm.getOptions().size(); i++) {
      existingOptionsBuilder.add(
          multiOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      multiOptionQuestionForm.getOptionIds().get(i),
                      i,
                      multiOptionQuestionForm.getOptions().get(i),
                      LocalizedStrings.DEFAULT_LOCALE))));
    }

    content
        .with(existingOptionsBuilder.build())
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
  private static ImmutableMap<String, String> stateOptions() {
    return ImmutableMap.of("-- Leave blank --", "-", "Washington", "WA");
  }
}
