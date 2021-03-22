package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.QuestionForm;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.Styles;

public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content renderNewQuestionForm(Request request, QuestionType questionType) {
    String type = questionType.toString();
    QuestionForm form = new QuestionForm();
    form.setQuestionType(type.toUpperCase());
    return layout.render(
        body(
            renderHeader(String.format("New %s question", type.toLowerCase()), Styles.CAPITALIZE),
            buildNewQuestionForm(form).with(makeCsrfTokenInputTag(request))));
  }

  public Content renderNewQuestionForm(Request request, QuestionForm questionForm, String message) {
    return layout.render(
        body(
            div(message),
            renderHeader("New text question"),
            buildNewQuestionForm(questionForm).with(makeCsrfTokenInputTag(request))));
  }

  public Content renderEditQuestionForm(Request request, QuestionDefinition question) {
    String type = question.getQuestionType().toString();
    return layout.render(
        body(
            renderHeader(String.format("Edit %s question", type.toLowerCase())),
            buildEditQuestionForm(question.getId(), new QuestionForm(question))
                .with(makeCsrfTokenInputTag(request))));
  }

  public Content renderEditQuestionForm(
      Request request, long id, QuestionForm questionForm, String message) {
    String type = questionForm.getQuestionType();
    return layout.render(
        body(
            div(message),
            renderHeader(String.format("Edit %s question", type.toLowerCase())),
            buildEditQuestionForm(id, questionForm).with(makeCsrfTokenInputTag(request))));
  }

  private ContainerTag buildNewQuestionForm(QuestionForm questionForm) {
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag
        .withAction(controllers.admin.routes.QuestionController.create().url())
        .with(submitButton("Create").withClass(Styles.ML_2));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(long id, QuestionForm questionForm) {
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag
        .withAction(controllers.admin.routes.QuestionController.update(id).url())
        .with(submitButton("Update").withClass(Styles.ML_2));
    return formTag;
  }

  private ContainerTag buildQuestionForm(QuestionForm questionForm) {
    QuestionType questionType = QuestionType.valueOf(questionForm.getQuestionType());
    ContainerTag formTag = form().withMethod("POST");
    formTag
        .with(
            FieldWithLabel.input()
                .setId("question-name-input")
                .setFieldName("questionName")
                .setLabelText("Name")
                .setPlaceholderText("The name displayed in the question builder")
                .setValue(questionForm.getQuestionName())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("question-description-textarea")
                .setFieldName("questionDescription")
                .setLabelText("Description")
                .setPlaceholderText("The description displayed in the question builder")
                .setValue(questionForm.getQuestionDescription())
                .getContainer(),
            FieldWithLabel.input()
                .setId("question-path-input")
                .setFieldName("questionPath")
                .setLabelText("Path")
                .setPlaceholderText("The path used to store question data")
                .setValue(questionForm.getQuestionPath().path())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("question-text-textarea")
                .setFieldName("questionText")
                .setLabelText("Question text")
                .setPlaceholderText("The question text displayed to the applicant")
                .setValue(questionForm.getQuestionText())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("question-help-text-textarea")
                .setFieldName("questionHelpText")
                .setLabelText("Question help text")
                .setPlaceholderText("The question help text displayed to the applicant")
                .setValue(questionForm.getQuestionHelpText())
                .getContainer())
        .with(formQuestionTypeSelect(questionType));

    formTag.with(QuestionConfig.buildQuestionConfig(questionType));
    return formTag;
  }

  private DomContent formQuestionTypeSelect(QuestionType selectedType) {

    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(QuestionType.values())
            .map(item -> new SimpleEntry<String, String>(item.toString(), item.name()))
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
}
