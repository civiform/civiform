package views.admin.questions;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.AddressQuestionForm;
import forms.EnumeratorQuestionForm;
import forms.IdQuestionForm;
import forms.MultiOptionQuestionForm;
import forms.NumberQuestionForm;
import forms.QuestionForm;
import forms.TextQuestionForm;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.OptionalLong;
import play.i18n.Messages;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Contains methods for rendering type-specific question settings components. */
public final class QuestionConfig {

  private static final String INNER_DIV_CLASSES =
      StyleUtils.joinStyles(
          "border", "bg-gray-100",
          "p-4", "m-4");

  private static final String OUTER_DIV_CLASSES =
      StyleUtils.joinStyles("w-full", "pt-0", "-mt-4");

  private DivTag content = div();

  private QuestionConfig() {}

  public static Optional<DivTag> buildQuestionConfig(QuestionForm questionForm, Messages messages) {
    QuestionConfig config = new QuestionConfig();
    switch (questionForm.getQuestionType()) {
      case ADDRESS:
        return Optional.of(
            config.addAddressQuestionConfig((AddressQuestionForm) questionForm).getContainer());
      case CHECKBOX:
        MultiOptionQuestionForm form = (MultiOptionQuestionForm) questionForm;
        return Optional.of(
            config
                .addMultiOptionQuestionFields(form, messages)
                .addMultiSelectQuestionValidation(form)
                .getContainer());
      case ENUMERATOR:
        return Optional.of(
            config
                .addEnumeratorQuestionConfig((EnumeratorQuestionForm) questionForm)
                .getContainer());
      case ID:
        return Optional.of(
            config.addIdQuestionConfig((IdQuestionForm) questionForm).getContainer());
      case NUMBER:
        return Optional.of(
            config.addNumberQuestionConfig((NumberQuestionForm) questionForm).getContainer());
      case TEXT:
        return Optional.of(
            config.addTextQuestionConfig((TextQuestionForm) questionForm).getContainer());
      case DROPDOWN: // fallthrough to RADIO_BUTTON
      case RADIO_BUTTON:
        return Optional.of(
            config
                .addMultiOptionQuestionFields((MultiOptionQuestionForm) questionForm, messages)
                .getContainer());
      case CURRENCY: // fallthrough intended - no options
      case FILEUPLOAD: // fallthrough intended
      case NAME: // fallthrough intended - no options
      case DATE: // fallthrough intended
      case EMAIL: // fallthrough intended
      case STATIC:
      default:
        return Optional.empty();
    }
  }

  private QuestionConfig addAddressQuestionConfig(AddressQuestionForm addressQuestionForm) {
    content.with(
        new SelectWithLabel()
            .setFieldName("defaultState")
            .setLabelText("Default state")
            .setOptions(stateOptions())
            .setValue("-")
            .getSelectTag(),
        FieldWithLabel.checkbox()
            .setFieldName("disallowPoBox")
            .setLabelText("Disallow post office boxes")
            .setValue("true")
            .setChecked(addressQuestionForm.getDisallowPoBox())
            .getCheckboxTag());
    return this;
  }

  private QuestionConfig addIdQuestionConfig(IdQuestionForm idQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setFieldName("minLength")
            .setLabelText("Minimum length")
            .setValue(idQuestionForm.getMinLength())
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("maxLength")
            .setLabelText("Maximum length")
            .setValue(idQuestionForm.getMaxLength())
            .getNumberTag());
    return this;
  }

  private QuestionConfig addTextQuestionConfig(TextQuestionForm textQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setFieldName("minLength")
            .setLabelText("Minimum length")
            .setValue(textQuestionForm.getMinLength())
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("maxLength")
            .setLabelText("Maximum length")
            .setValue(textQuestionForm.getMaxLength())
            .getNumberTag());
    return this;
  }

  private QuestionConfig addEnumeratorQuestionConfig(
      EnumeratorQuestionForm enumeratorQuestionForm) {
    content.with(
        FieldWithLabel.input()
            .setId("enumerator-question-entity-type-input")
            .setFieldName("entityType")
            .setLabelText("Repeated entity type*")
            .setPlaceholderText("What are we enumerating?")
            .setValue(enumeratorQuestionForm.getEntityType())
            .getInputTag());
    return this;
  }

  /**
   * Creates a template text field where an admin can enter a single multi-option question answer,
   * along with a button to remove the option.
   */
  public static DivTag multiOptionQuestionFieldTemplate(Messages messages) {
    return multiOptionQuestionField(Optional.empty(), messages, /* isForNewOption= */ true);
  }

  /**
   * Creates an individual text field where an admin can enter a single multi-option question
   * answer, along with a button to remove the option.
   */
  private static DivTag multiOptionQuestionField(
      Optional<LocalizedQuestionOption> existingOption, Messages messages, boolean isForNewOption) {
    DivTag optionInput =
        FieldWithLabel.input()
            .setFieldName(isForNewOption ? "newOptions[]" : "options[]")
            .setLabelText("Question option*")
            .addReferenceClass(ReferenceClasses.MULTI_OPTION_INPUT)
            .setValue(existingOption.map(LocalizedQuestionOption::optionText))
            .setFieldErrors(
                messages,
                ImmutableSet.of(ValidationErrorMessage.create(MessageKey.MULTI_OPTION_VALIDATION)))
            .showFieldErrors(false)
            .getInputTag()
            .withClasses("flex", "ml-2", "gap-x-3");
    DivTag optionIndexInput =
        isForNewOption
            ? div()
            : FieldWithLabel.input()
                .setFieldName("optionIds[]")
                .setValue(String.valueOf(existingOption.get().id()))
                .setScreenReaderText("option ids")
                .getInputTag()
                .withClasses("hidden");
    ButtonTag removeOptionButton =
        button("Remove")
            .withType("button")
            .withClasses("flex", "ml-4", "multi-option-question-field-remove-button");

    return div()
        .withClasses(
            ReferenceClasses.MULTI_OPTION_QUESTION_OPTION,
            "flex",
            "flex-row",
            "mb-4")
        .with(optionInput, optionIndexInput, removeOptionButton);
  }

  private QuestionConfig addMultiOptionQuestionFields(
      MultiOptionQuestionForm multiOptionQuestionForm, Messages messages) {
    Preconditions.checkState(
        multiOptionQuestionForm.getOptionIds().size()
            == multiOptionQuestionForm.getOptions().size(),
        "Options and Option Indexes need to be the same size.");
    ImmutableList.Builder<DivTag> optionsBuilder = ImmutableList.builder();
    int optionIndex = 0;
    for (int i = 0; i < multiOptionQuestionForm.getOptions().size(); i++) {
      optionsBuilder.add(
          multiOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      multiOptionQuestionForm.getOptionIds().get(i),
                      optionIndex,
                      multiOptionQuestionForm.getOptions().get(i),
                      LocalizedStrings.DEFAULT_LOCALE)),
              messages,
              /* isForNewOption= */ false));
      optionIndex++;
    }
    for (String newOption : multiOptionQuestionForm.getNewOptions()) {
      optionsBuilder.add(
          multiOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      -1, optionIndex, newOption, LocalizedStrings.DEFAULT_LOCALE)),
              messages,
              /* isForNewOption= */ true));
      optionIndex++;
    }

    content
        .with(optionsBuilder.build())
        .with(
            button("Add answer option")
                .withType("button")
                .withId("add-new-option")
                .withClasses("m-2"));
    return this;
  }

  /**
   * Creates two number input fields, where an admin can specify the min and max number of choices
   * allowed for multi-select questions.
   */
  private QuestionConfig addMultiSelectQuestionValidation(MultiOptionQuestionForm multiOptionForm) {
    content.with(
        FieldWithLabel.number()
            .setFieldName("minChoicesRequired")
            .setLabelText("Minimum number of choices required")
            // Negative numbers aren't allowed. Force the admin to provide
            // a positive number.
            .setMin(OptionalLong.of(0L))
            .setValue(multiOptionForm.getMinChoicesRequired())
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("maxChoicesAllowed")
            .setLabelText("Maximum number of choices allowed")
            // Negative numbers aren't allowed. Force the admin to provide
            // a positive number.
            .setMin(OptionalLong.of(0L))
            .setValue(multiOptionForm.getMaxChoicesAllowed())
            .getNumberTag());
    return this;
  }

  private QuestionConfig addNumberQuestionConfig(NumberQuestionForm numberQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setFieldName("min")
            .setLabelText("Minimum value")
            .setValue(numberQuestionForm.getMin())
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("max")
            .setLabelText("Maximum value")
            .setValue(numberQuestionForm.getMax())
            .getNumberTag());
    return this;
  }

  public DivTag getContainer() {
    return div()
        .withClasses(ReferenceClasses.QUESTION_CONFIG)
        .with(
            div()
                .withClasses(OUTER_DIV_CLASSES)
                .with(content.withId("question-settings").withClasses(INNER_DIV_CLASSES)));
  }

  /**
   * I don't feel like hard-coding a list of states here, so this will do until we can think up a
   * better approach.
   */
  private static ImmutableList<SelectWithLabel.OptionValue> stateOptions() {
    return ImmutableList.of(
        SelectWithLabel.OptionValue.builder().setLabel("-- Leave blank --").setValue("-").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Washington").setValue("WA").build());
  }
}
