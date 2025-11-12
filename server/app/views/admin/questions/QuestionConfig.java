package views.admin.questions;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;
import static j2html.TagCreator.strong;
import static j2html.TagCreator.text;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.AddressQuestionForm;
import forms.DateQuestionForm;
import forms.EnumeratorQuestionForm;
import forms.FileUploadQuestionForm;
import forms.IdQuestionForm;
import forms.MultiOptionQuestionForm;
import forms.NumberQuestionForm;
import forms.QuestionForm;
import forms.TextQuestionForm;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import java.util.Optional;
import java.util.OptionalLong;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import services.question.YesNoQuestionOption;
import services.question.types.DateQuestionDefinition.DateValidationOption;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.settings.SettingsManifest;
import views.ViewUtils;
import views.admin.BaseView;
import views.admin.BaseViewModel;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.SelectWithLabel;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Contains methods for rendering type-specific question settings components. */
public final class QuestionConfig {

  private static final String INNER_DIV_CLASSES =
      StyleUtils.joinStyles("border", "bg-gray-100", "p-4", "m-4", "border-gray-200");

  private static final String OUTER_DIV_CLASSES = StyleUtils.joinStyles("w-full", "pt-0", "-mt-4");

  private final DivTag content = div();

  private QuestionConfig() {}

  public static Optional<DivTag> buildQuestionConfig(
      QuestionForm questionForm,
      Messages messages,
      SettingsManifest settingsManifest,
      Request request) {
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
      case YES_NO:
        return Optional.of(
            config
                .addDefaultYesNoQuestionFields((MultiOptionQuestionForm) questionForm)
                .getContainer());
      case DROPDOWN: // fallthrough to RADIO_BUTTON
      case RADIO_BUTTON:
        return Optional.of(
            config
                .addMultiOptionQuestionFields((MultiOptionQuestionForm) questionForm, messages)
                .getContainer());
      case FILEUPLOAD:
        return Optional.of(
            config
                .addFileUploadQuestionFields((FileUploadQuestionForm) questionForm)
                .getContainer());
      case DATE:
        return Optional.of(
            config.addDateQuestionConfig((DateQuestionForm) questionForm, messages).getContainer());
      case MAP: // fallthrough intended - MAP question configuration is handled in
        // QuestionEditView.getQuestionConfig
      case CURRENCY: // fallthrough intended - no options
      case NAME: // fallthrough intended - no options
      case EMAIL: // fallthrough intended
      case STATIC:
      default:
        return Optional.empty();
    }
  }

  public static <TModel extends BaseViewModel> Optional<DivTag> buildQuestionConfigUsingThymeleaf(
      Request request, BaseView<TModel> view, TModel model) {
    return Optional.of(new QuestionConfig().addConfig(request, view, model).getContainer());
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
            .setMin(OptionalLong.of(0))
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("maxLength")
            .setLabelText("Maximum length")
            .setValue(idQuestionForm.getMaxLength())
            .setMin(OptionalLong.of(1))
            .getNumberTag());
    return this;
  }

  private QuestionConfig addTextQuestionConfig(TextQuestionForm textQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setFieldName("minLength")
            .setLabelText("Minimum length")
            .setValue(textQuestionForm.getMinLength())
            .setMin(OptionalLong.of(0))
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("maxLength")
            .setLabelText("Maximum length")
            .setValue(textQuestionForm.getMaxLength())
            .setMin(OptionalLong.of(1))
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

  private QuestionConfig addDateQuestionConfig(
      DateQuestionForm dateQuestionForm, Messages messages) {
    DateType minDateType = dateQuestionForm.getMinDateType().orElse(DateType.ANY);
    DateType maxDateType = dateQuestionForm.getMaxDateType().orElse(DateType.ANY);
    DivTag minDateSelectInput =
        new SelectWithLabel()
            .setId("min-date-type")
            .setFieldName("minDateType")
            .setLabelText("Start date")
            .setOptions(dateValidationOptions(/* anyDateOptionLabel= */ "Any past date"))
            .setValue(minDateType.toString())
            .setRequired(true)
            .getSelectTag();
    DivTag maxDateSelectInput =
        new SelectWithLabel()
            .setId("max-date-type")
            .setFieldName("maxDateType")
            .setLabelText("End date")
            .setOptions(dateValidationOptions(/* anyDateOptionLabel= */ "Any future date"))
            .setValue(maxDateType.toString())
            .setRequired(true)
            .getSelectTag();

    FieldsetTag minCustomDatePicker =
        ViewUtils.makeMemorableDate(
                /* hideDateComponent= */ !minDateType.equals(DateType.CUSTOM),
                dateQuestionForm.getMinCustomDay().orElse(""),
                dateQuestionForm.getMinCustomMonth().orElse(""),
                dateQuestionForm.getMinCustomYear().orElse(""),
                "min-custom-date",
                "minCustomDay",
                "minCustomMonth",
                "minCustomYear",
                /* showError= */ false,
                /* showRequired= */ true,
                Optional.of(messages))
            .withClass("pb-2");
    FieldsetTag maxCustomDatePicker =
        ViewUtils.makeMemorableDate(
                /* hideDateComponent= */ !maxDateType.equals(DateType.CUSTOM),
                dateQuestionForm.getMaxCustomDay().orElse(""),
                dateQuestionForm.getMaxCustomMonth().orElse(""),
                dateQuestionForm.getMaxCustomYear().orElse(""),
                "max-custom-date",
                "maxCustomDay",
                "maxCustomMonth",
                "maxCustomYear",
                /* showError= */ false,
                /* showRequired= */ true,
                Optional.of(messages))
            .withClass("pb-2");

    content
        .with(
            legend("Validation parameters").withClass(BaseStyles.INPUT_LABEL),
            p().withClasses("px-1", "pb-2", "text-sm", "text-gray-600")
                .with(
                    span("Set the parameters for allowable values for this date question below.")))
        .with(minDateSelectInput, minCustomDatePicker, maxDateSelectInput, maxCustomDatePicker);
    return this;
  }

  private ImmutableList<SelectWithLabel.OptionValue> dateValidationOptions(
      String anyDateOptionLabel) {
    return ImmutableList.<SelectWithLabel.OptionValue>builder()
        .add(
            SelectWithLabel.OptionValue.builder()
                .setLabel(anyDateOptionLabel)
                .setValue(DateValidationOption.DateType.ANY.toString())
                .build())
        .add(
            SelectWithLabel.OptionValue.builder()
                .setLabel("Current date of application")
                .setValue(DateValidationOption.DateType.APPLICATION_DATE.toString())
                .build())
        .add(
            SelectWithLabel.OptionValue.builder()
                .setLabel("Custom date")
                .setValue(DateValidationOption.DateType.CUSTOM.toString())
                .build())
        .build();
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
                      /* displayInAnswerOptions= */ Optional.of(true),
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
                      /* displayInAnswerOptions= */ Optional.of(true),
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

  private QuestionConfig addDefaultYesNoQuestionFields(
      MultiOptionQuestionForm multiOptionQuestionForm) {
    Preconditions.checkState(
        multiOptionQuestionForm.getOptionIds().size()
            == multiOptionQuestionForm.getOptions().size(),
        "Options and Option indexes need to be the same size.");
    ImmutableList.Builder<DivTag> optionsBuilder = ImmutableList.builder();
    if (multiOptionQuestionForm.getOptions().size() == 0) {
      optionsBuilder.add(
          div()
              .with(
                  label()
                      .withText("Select answer options")
                      .withData("testId", "yes-no-options-label")
                      .with(ViewUtils.requiredQuestionIndicator())
                      .withClasses("text-sm", "font-medium", "text-gray-700")));
      optionsBuilder.add(
          yesNoOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      /* id= */ YesNoQuestionOption.YES.getId(),
                      /* order= */ 0,
                      /* adminName= */ YesNoQuestionOption.YES.getAdminName(),
                      /* optionText= */ "Yes",
                      /* displayInAnswerOptions= */ Optional.of(true),
                      LocalizedStrings.DEFAULT_LOCALE))));
      optionsBuilder.add(
          yesNoOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      /* id= */ YesNoQuestionOption.NO.getId(),
                      /* order= */ 1,
                      /* adminName= */ YesNoQuestionOption.NO.getAdminName(),
                      /* optionText= */ "No",
                      /* displayInAnswerOptions= */ Optional.of(true),
                      LocalizedStrings.DEFAULT_LOCALE))));
      optionsBuilder.add(
          yesNoOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      /* id= */ YesNoQuestionOption.NOT_SURE.getId(),
                      /* order= */ 2,
                      /* adminName= */ YesNoQuestionOption.NOT_SURE.getAdminName(),
                      /* optionText= */ "Not sure",
                      /* displayInAnswerOptions= */ Optional.of(true),
                      LocalizedStrings.DEFAULT_LOCALE))));
      optionsBuilder.add(
          yesNoOptionQuestionField(
              Optional.of(
                  LocalizedQuestionOption.create(
                      /* id= */ YesNoQuestionOption.MAYBE.getId(),
                      /* order= */ 3,
                      /* adminName= */ YesNoQuestionOption.MAYBE.getAdminName(),
                      /* optionText= */ "Maybe",
                      /* displayInAnswerOptions= */ Optional.of(true),
                      LocalizedStrings.DEFAULT_LOCALE))));
    } else {
      for (int i = 0; i < multiOptionQuestionForm.getOptions().size(); i++) {
        optionsBuilder.add(
            yesNoOptionQuestionField(
                Optional.of(
                    LocalizedQuestionOption.create(
                        multiOptionQuestionForm.getOptionIds().get(i),
                        i,
                        multiOptionQuestionForm.getOptionAdminNames().get(i),
                        multiOptionQuestionForm.getOptions().get(i),
                        /* displayInAnswerOptions= */ Optional.of(
                            multiOptionQuestionForm
                                .getDisplayedOptionIds()
                                .contains(multiOptionQuestionForm.getOptionIds().get(i))),
                        LocalizedStrings.DEFAULT_LOCALE))));
      }
    }
    content.with(optionsBuilder.build());
    return this;
  }

  private static DivTag yesNoOptionQuestionField(Optional<LocalizedQuestionOption> existingOption) {
    String adminName = existingOption.map(LocalizedQuestionOption::adminName).get();

    // Hidden inputs allow Play's form binding to submit the input value, while we show static text
    // to the admin.
    InputTag optionAdminNameHidden =
        input().withName("optionAdminNames[]").withValue(adminName).withClasses("display-none");
    InputTag optionIdsHiddenInput =
        input()
            .withName("optionIds[]")
            .withValue(String.valueOf(existingOption.get().id()))
            .withClasses("display-none");
    InputTag optionTextHiddenInput =
        input()
            .withName("options[]")
            .withValue(existingOption.map(LocalizedQuestionOption::optionText).get())
            .withClasses("display-none");

    DivTag adminNameDiv = div(strong("Admin ID: "), text(adminName));
    DivTag optionTextDiv =
        div(
            strong("Option text: "),
            text(existingOption.map(LocalizedQuestionOption::optionText).get()));

    boolean isRequiredOption = YesNoQuestionOption.getRequiredAdminNames().contains(adminName);

    boolean isChecked =
        existingOption.get().displayInAnswerOptions().isPresent()
            && existingOption.get().displayInAnswerOptions().get();

    String ariaLabel =
        String.format(
            "Admin ID: %s. Option text: %s.",
            existingOption.map(LocalizedQuestionOption::adminName).get(),
            existingOption.map(LocalizedQuestionOption::optionText).get());
    LabelTag label =
        label()
            .with(div().with(adminNameDiv, optionTextDiv).withClasses("flex-column"))
            .withFor(adminName)
            .attr("aria-label", ariaLabel)
            .withClasses("usa-checkbox__label", "margin-top-0", "flex", "flex-align-center");

    // Checkbox for selecting whether to display the option to the applicant.
    // Value is set to the ID because falsy checkbox values get discarded on form
    // submission.
    InputTag checkboxInput =
        input()
            .withId(existingOption.map(LocalizedQuestionOption::adminName).get())
            .withType("checkbox")
            .withName("displayedOptionIds[]")
            .withValue(Long.toString(existingOption.get().id()))
            .withClasses("usa-checkbox__input");

    // Apply disabled and checked for required options
    if (isRequiredOption) {
      checkboxInput.attr("checked", "checked").attr("disabled", "disabled");
    } else {
      checkboxInput.withCondChecked(isChecked);
    }

    DivTag checkboxWithLabels =
        div()
            .with(checkboxInput, label)
            .withClasses(
                "usa-checkbox",
                "flex-align-center",
                "border-width-1px",
                "border-base",
                "radius-md",
                "grid-row",
                "items-center",
                "padding-1",
                "margin-1");

    DivTag container =
        div()
            .withClasses(ReferenceClasses.MULTI_OPTION_QUESTION_OPTION, "grid", "items-center")
            .with(
                optionIdsHiddenInput,
                optionAdminNameHidden,
                optionTextHiddenInput,
                checkboxWithLabels);

    // Add extra hidden input for required options to ensure value is submitted
    if (isRequiredOption) {
      container.with(
          input()
              .withType("hidden")
              .withName("displayedOptionIds[]")
              .withValue(Long.toString(existingOption.get().id())));
    }

    return container;
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
            .setMin(OptionalLong.of(0L))
            .setValue(multiOptionForm.getMinChoicesRequired())
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("maxChoicesAllowed")
            .setLabelText("Maximum number of choices allowed")
            .setMin(OptionalLong.of(1L))
            .setValue(multiOptionForm.getMaxChoicesAllowed())
            .getNumberTag());
    return this;
  }

  private <TModel extends BaseViewModel> QuestionConfig addConfig(
      Request request, BaseView<TModel> view, TModel model) {
    content.with(rawHtml(view.render(request, model)));
    return this;
  }

  private QuestionConfig addNumberQuestionConfig(NumberQuestionForm numberQuestionForm) {
    content.with(
        FieldWithLabel.number()
            .setFieldName("min")
            .setLabelText("Minimum value")
            .setMin(OptionalLong.of(0L))
            .setValue(numberQuestionForm.getMin())
            .getNumberTag(),
        FieldWithLabel.number()
            .setFieldName("max")
            .setLabelText("Maximum value")
            .setMin(OptionalLong.of(0L))
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
                .with(
                    content
                        .withId("question-settings")
                        .attr("data-testid", "question-settings")
                        .withClasses(INNER_DIV_CLASSES)));
  }
}
