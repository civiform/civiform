package views.admin.questions;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.AddressQuestionForm;
import forms.EnumeratorQuestionForm;
import forms.FileUploadQuestionForm;
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
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Contains methods for rendering type-specific question settings components. */
public final class QuestionConfig {

  private static final String INNER_DIV_CLASSES =
      StyleUtils.joinStyles(
          "border", "bg-gray-100",
          "p-4", "m-4");

  private static final String OUTER_DIV_CLASSES = StyleUtils.joinStyles("w-full", "pt-0", "-mt-4");

  private final DivTag content = div();

  private QuestionConfig() {}

  public static Optional<DivTag> buildQuestionConfig(
      QuestionForm questionForm, Messages messages, boolean multipleFileUpload) {
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
      case PHONE:
        return Optional.of(config.addPhoneConfig().getContainer());
      case DROPDOWN: // fallthrough to RADIO_BUTTON
      case RADIO_BUTTON:
        return Optional.of(
            config
                .addMultiOptionQuestionFields((MultiOptionQuestionForm) questionForm, messages)
                .getContainer());
      case FILEUPLOAD:
        return multipleFileUpload
            ? Optional.of(
                config
                    .addFileUploadQuestionFields((FileUploadQuestionForm) questionForm)
                    .getContainer())
            : Optional.empty();
      case CURRENCY: // fallthrough intended - no options
      case NAME: // fallthrough intended - no options
      case DATE: // fallthrough intended
      case EMAIL: // fallthrough intended
      case STATIC:
      default:
        return Optional.empty();
    }
  }

  private QuestionConfig addPhoneConfig() {
    content.with(
        new DivTag()
            .withText(
                "This supports only US and CA phone numbers. If you need other international"
                    + " numbers, please use a Text question."));
    return this;
  }

  private QuestionConfig addAddressQuestionConfig(AddressQuestionForm addressQuestionForm) {
    content.with(
        FieldWithLabel.checkbox()
            .setFieldName("disallowPoBox")
            .setLabelText("Disallow post office boxes")
            .setValue("true")
            .setChecked(addressQuestionForm.getDisallowPoBox())
            .getCheckboxTag());
    return this;
  }

  private QuestionConfig addFileUploadQuestionFields(
      FileUploadQuestionForm fileUploadQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setFieldName("maxFiles")
            .setLabelText("Maximum number of file uploads")
            .setValue(fileUploadQuestionForm.getMaxFiles())
            .setMin(OptionalLong.of(1))
            .getNumberTag());
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
            .setLabelText("Repeated entity type (What are we enumerating?)")
            .setRequired(true)
            .setValue(enumeratorQuestionForm.getEntityType())
            .getInputTag(),
        FieldWithLabel.number()
            .setFieldName("minEntities")
            .setLabelText("Minimum entity count")
            .setRequired(false)
            .setValue(enumeratorQuestionForm.getMinEntities())
            .setMin(OptionalLong.of(0))
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("maxEntities")
            .setLabelText("Maximum entity count")
            .setRequired(false)
            .setValue(enumeratorQuestionForm.getMaxEntities())
            .setMin(OptionalLong.of(1))
            .getNumberTag());
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
    DivTag optionAdminName =
        FieldWithLabel.input()
            .setFieldName(isForNewOption ? "newOptionAdminNames[]" : "optionAdminNames[]")
            .setLabelText("Admin ID")
            .setRequired(true)
            .addReferenceClass(ReferenceClasses.MULTI_OPTION_ADMIN_INPUT)
            .setValue(existingOption.map(LocalizedQuestionOption::adminName))
            .setFieldErrors(
                messages,
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.MULTI_OPTION_ADMIN_VALIDATION)))
            .showFieldErrors(false)
            .setReadOnly(!isForNewOption)
            .getInputTag()
            .withClasses(
                ReferenceClasses.MULTI_OPTION_ADMIN_INPUT,
                "col-start-1",
                "col-span-5",
                "mb-2",
                "ml-2",
                "row-start-1",
                "row-span-2");
    DivTag optionIndexInput =
        isForNewOption
            ? div()
            : FieldWithLabel.input()
                .setFieldName("optionIds[]")
                .setValue(String.valueOf(existingOption.get().id()))
                .setScreenReaderText("option ids")
                .getInputTag()
                .withClasses("hidden");
    ButtonTag moveUpButton =
        button()
            .with(Icons.svg(Icons.KEYBOARD_ARROW_UP).withClasses("w-6", "h-6"))
            .withClasses(
                AdminStyles.MOVE_BLOCK_BUTTON,
                "col-start-8",
                "multi-option-question-field-move-up-button",
                "row-start-1")
            .attr("aria-label", "move up");
    ButtonTag moveDownButton =
        button()
            .with(Icons.svg(Icons.KEYBOARD_ARROW_DOWN).withClasses("w-6", "h-6"))
            .withClasses(
                AdminStyles.MOVE_BLOCK_BUTTON,
                "col-start-8",
                "multi-option-question-field-move-down-button",
                "row-start-2")
            .attr("aria-label", "move down");
    DivTag optionInput =
        FieldWithLabel.input()
            .setFieldName(isForNewOption ? "newOptions[]" : "options[]")
            .setLabelText("Option Text")
            .setRequired(true)
            .addReferenceClass(ReferenceClasses.MULTI_OPTION_INPUT)
            .setMarkdownSupported(true)
            .setMarkdownText("Some markdown is supported, ")
            .setMarkdownLinkText("see how it works")
            .setValue(existingOption.map(LocalizedQuestionOption::optionText))
            .setFieldErrors(
                messages,
                ImmutableSet.of(ValidationErrorMessage.create(MessageKey.MULTI_OPTION_VALIDATION)))
            .showFieldErrors(false)
            .getInputTag()
            .withClasses(
                ReferenceClasses.MULTI_OPTION_INPUT,
                "col-start-1",
                "col-span-5",
                "mb-2",
                "ml-2",
                "row-start-3",
                "row-span-2");
    ButtonTag removeOptionButton =
        button()
            .with(Icons.svg(Icons.DELETE).withClasses("w-6", "h-6"))
            .withClasses(
                AdminStyles.DELETE_ICON_BUTTON,
                "multi-option-question-field-remove-button",
                "col-start-8",
                "row-span-2")
            .attr("aria-label", "delete");
    return div()
        .withClasses(
            ReferenceClasses.MULTI_OPTION_QUESTION_OPTION,
            ReferenceClasses.MULTI_OPTION_QUESTION_OPTION_EDITABLE,
            "grid",
            "grid-cols-8",
            "grid-rows-4",
            "items-center",
            "mb-4")
        .with(
            optionIndexInput,
            optionAdminName,
            moveUpButton,
            moveDownButton,
            optionInput,
            removeOptionButton);
  }

  private QuestionConfig addMultiOptionQuestionFields(
      MultiOptionQuestionForm multiOptionQuestionForm, Messages messages) {
    Preconditions.checkState(
        multiOptionQuestionForm.getOptionIds().size()
            == multiOptionQuestionForm.getOptions().size(),
        "Options and Option indexes need to be the same size.");
    ImmutableList.Builder<DivTag> optionsBuilder = ImmutableList.builder();
    int optionIndex = 0;
    for (int i = 0; i < multiOptionQuestionForm.getOptions().size(); i++) {
      optionsBuilder.add(
          multiOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      multiOptionQuestionForm.getOptionIds().get(i),
                      optionIndex,
                      multiOptionQuestionForm.getOptionAdminNames().get(i),
                      multiOptionQuestionForm.getOptions().get(i),
                      LocalizedStrings.DEFAULT_LOCALE)),
              messages,
              /* isForNewOption= */ false));
      optionIndex++;
    }

    for (int i = 0; i < multiOptionQuestionForm.getNewOptions().size(); i++) {
      optionsBuilder.add(
          multiOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      -1,
                      optionIndex,
                      multiOptionQuestionForm.getNewOptionAdminNames().get(i),
                      multiOptionQuestionForm.getNewOptions().get(i),
                      LocalizedStrings.DEFAULT_LOCALE)),
              messages,
              /* isForNewOption= */ true));
      optionIndex++;
    }

    content
        .with(optionsBuilder.build())
        .with(
            ViewUtils.makeSvgTextButton("Add answer option", Icons.PLUS)
                .withType("button")
                .withId("add-new-option")
                .withClasses("m-2", ButtonStyles.OUTLINED_WHITE_WITH_ICON))
        .with(
            FieldWithLabel.number()
                .setFieldName("nextAvailableId")
                .setValue(multiOptionQuestionForm.getNextAvailableId())
                .getNumberTag()
                .withClasses("hidden"));
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
}
