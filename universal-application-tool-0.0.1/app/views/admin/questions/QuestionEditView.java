package views.admin.questions;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.main;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.AddressQuestionForm;
import forms.CheckboxQuestionForm;
import forms.DropdownQuestionForm;
import forms.QuestionForm;
import forms.RadioButtonQuestionForm;
import forms.TextQuestionForm;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Optional;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.RadioButtonQuestionDefinition;
import services.question.types.TextQuestionDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.Styles;

public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionEditView(AdminLayout layout) {
    this.layout = layout;
  }

  /** Render a fresh New Question Form. */
  public Content renderNewQuestionForm(Request request, QuestionType questionType) {
    QuestionForm questionForm = getFreshQuestionForm(questionType);
    return renderNewQuestionForm(request, questionForm, Optional.empty());
  }

  /** Render a New Question Form with a partially filled form and an error message. */
  public Content renderNewQuestionForm(
      Request request, QuestionForm questionForm, String errorMessage) {
    return renderNewQuestionForm(request, questionForm, Optional.of(errorMessage));
  }

  private Content renderNewQuestionForm(
      Request request, QuestionForm questionForm, Optional<String> message) {
    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("New %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title)
            .with(buildNewQuestionForm(questionForm).with(makeCsrfTokenInputTag(request)));
    ContainerTag previewContent = buildPreviewContent(questionType);
    ContainerTag mainContent = main(formContent, previewContent);

    if (message.length() > 0) {
      mainContent.with(ToastMessage.error(message).setDismissible(false).getContainerTag());
    }

    return layout.renderFull(mainContent);
  }

  public Content renderEditQuestionForm(Request request, QuestionDefinition questionDefinition) {
    QuestionType questionType = questionDefinition.getQuestionType();
    QuestionForm questionForm = getQuestionFormFromQuestionDefinition(questionDefinition);

    String title = String.format("Edit %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title)
            .with(
                buildEditQuestionForm(questionDefinition.getId(), questionForm)
                    .with(makeCsrfTokenInputTag(request)));
    ContainerTag previewContent = buildPreviewContent(questionType);
    ContainerTag mainContent = main(formContent, previewContent);

    return layout.renderFull(mainContent);
  }

  public Content renderEditQuestionForm(
      Request request, long id, QuestionForm questionForm, String message) {
    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("Edit %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title)
            .with(buildEditQuestionForm(id, questionForm).with(makeCsrfTokenInputTag(request)));
    ContainerTag previewContent = buildPreviewContent(questionType);
    ContainerTag mainContent = main(formContent, previewContent);

    if (message.length() > 0) {
      mainContent.with(ToastMessage.error(message).setDismissible(false).getContainerTag());
    }

    return layout.renderFull(mainContent);
  }

  public Content renderViewQuestionForm(QuestionDefinition question) {
    QuestionType questionType = question.getQuestionType();
    QuestionForm questionForm = getQuestionFormFromQuestionDefinition(question);
    String title = String.format("View %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title).with(buildViewOnlyQuestionForm(questionForm));
    ContainerTag previewContent = buildPreviewContent(questionType);
    ContainerTag mainContent = main(formContent, previewContent);

    return layout.renderFull(mainContent);
  }

  private ContainerTag buildSubmittableQuestionForm(QuestionForm questionForm) {
    return buildQuestionForm(questionForm, true);
  }

  private ContainerTag buildViewOnlyQuestionForm(QuestionForm questionForm) {
    return buildQuestionForm(questionForm, false);
  }

  private ContainerTag buildQuestionContainer(String title) {
    return div()
        .withId("question-form")
        .withClasses(
            Styles.BORDER_GRAY_400,
            Styles.BORDER_R,
            Styles.FLEX,
            Styles.FLEX_COL,
            Styles.H_FULL,
            Styles.OVERFLOW_HIDDEN,
            Styles.OVERFLOW_Y_AUTO,
            Styles.RELATIVE,
            Styles.W_2_5)
        .with(renderHeader(title, Styles.CAPITALIZE))
        .with(multiOptionQuestionField());
  }

  // A hidden template for multi-option questions.
  private ContainerTag multiOptionQuestionField() {
    return QuestionConfig.multiOptionQuestionField(Optional.empty())
        .withId("multi-option-question-answer-template")
        // Add "hidden" to other classes, so that the template is not shown
        .withClasses(Styles.HIDDEN, Styles.FLEX, Styles.FLEX_ROW, Styles.MB_4);
  }

  private ContainerTag buildPreviewContent(QuestionType questionType) {
    return QuestionPreview.renderQuestionPreview(questionType);
  }

  private ContainerTag buildNewQuestionForm(QuestionForm questionForm) {
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm);
    formTag
        .withAction(
            controllers.admin.routes.QuestionController.create(
                    questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Create").withClass(Styles.ML_2));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(long id, QuestionForm questionForm) {
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm);
    formTag
        .withAction(
            controllers.admin.routes.QuestionController.update(
                    id, questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Update").withClass(Styles.ML_2));
    return formTag;
  }

  private ContainerTag buildQuestionForm(QuestionForm questionForm, boolean submittable) {
    QuestionType questionType = questionForm.getQuestionType();
    ContainerTag formTag = form().withMethod("POST");
    FieldWithLabel nameField =
        FieldWithLabel.input()
            .setId("question-name-input")
            .setFieldName("questionName")
            .setLabelText("Name")
            .setDisabled(!submittable)
            .setPlaceholderText("The name displayed in the question builder")
            .setValue(questionForm.getQuestionName());
    if (Strings.isNullOrEmpty(questionForm.getQuestionName())) {
      formTag.with(nameField.getContainer());
    } else {
      // If there is already a name, we need to disable the `name` field but we
      // need to add a hidden input to send the same name as well.
      formTag.with(
          nameField.setDisabled(true).getContainer(),
          input().isHidden().withValue(questionForm.getQuestionName()).withName("questionName"));
    }

    formTag
        .with(
            FieldWithLabel.textArea()
                .setId("question-description-textarea")
                .setFieldName("questionDescription")
                .setLabelText("Description")
                .setPlaceholderText("The description displayed in the question builder")
                .setDisabled(!submittable)
                .setValue(questionForm.getQuestionDescription())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("question-text-textarea")
                .setFieldName("questionText")
                .setLabelText("Question text")
                .setPlaceholderText("The question text displayed to the applicant")
                .setDisabled(!submittable)
                .setValue(questionForm.getQuestionText())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("question-help-text-textarea")
                .setFieldName("questionHelpText")
                .setLabelText("Question help text")
                .setPlaceholderText("The question help text displayed to the applicant")
                .setDisabled(!submittable)
                .setValue(questionForm.getQuestionHelpText())
                .getContainer())
        .with(formQuestionTypeSelect(questionType));

    formTag.with(QuestionConfig.buildQuestionConfig(questionType, questionForm));
    return formTag;
  }

  private DomContent formQuestionTypeSelect(QuestionType selectedType) {
    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(QuestionType.values())
            .map(item -> new SimpleEntry<>(item.toString(), item.name()))
            .collect(ImmutableList.toImmutableList());

    return new SelectWithLabel()
        .setId("question-type-select")
        .setFieldName("questionType")
        .setLabelText("Question type")
        .setOptions(options)
        .setValue(selectedType.name())
        .getContainer()
        .withClasses(Styles.HIDDEN);
  }

  private QuestionForm getFreshQuestionForm(QuestionType questionType) {
    QuestionForm questionForm;
    switch (questionType) {
      case ADDRESS:
        questionForm = new AddressQuestionForm();
        break;
      case CHECKBOX:
        questionForm = new CheckboxQuestionForm();
        break;
      case DROPDOWN:
        questionForm = new DropdownQuestionForm();
        break;
      case RADIO_BUTTON:
        questionForm = new RadioButtonQuestionForm();
        break;
      case TEXT:
        questionForm = new TextQuestionForm();
        break;
      default:
        questionForm = new QuestionForm();
    }
    // TODO(natsid): Remove the following line once we have question forms for each question type.
    questionForm.setQuestionType(questionType);
    return questionForm;
  }

  private QuestionForm getQuestionFormFromQuestionDefinition(
      QuestionDefinition questionDefinition) {
    QuestionType questionType = questionDefinition.getQuestionType();
    switch (questionType) {
      case ADDRESS:
        return new AddressQuestionForm((AddressQuestionDefinition) questionDefinition);
      case CHECKBOX:
        return new CheckboxQuestionForm((CheckboxQuestionDefinition) questionDefinition);
      case DROPDOWN:
        return new DropdownQuestionForm((DropdownQuestionDefinition) questionDefinition);
      case RADIO_BUTTON:
        return new RadioButtonQuestionForm((RadioButtonQuestionDefinition) questionDefinition);
      case TEXT:
        return new TextQuestionForm((TextQuestionDefinition) questionDefinition);
      default:
        return new QuestionForm(questionDefinition);
    }
  }
}
