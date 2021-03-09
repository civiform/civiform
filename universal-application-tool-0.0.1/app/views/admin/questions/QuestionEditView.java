package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
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

public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content renderNewQuestionForm(Request request) {
    return layout.render(
        body(
            renderHeader("New Question"),
            buildNewQuestionForm().with(makeCsrfTokenInputTag(request))));
  }

  public Content renderEditQuestionForm(Request request, QuestionDefinition question) {
    return layout.render(
        body(
            renderHeader("Edit Question"),
            buildEditQuestionForm(question).with(makeCsrfTokenInputTag(request))));
  }

  private ContainerTag buildNewQuestionForm() {
    QuestionForm questionForm = new QuestionForm();
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag
        .withAction(controllers.admin.routes.QuestionController.create().url())
        .with(submitButton("Create"));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(QuestionDefinition definition) {
    QuestionForm questionForm = new QuestionForm(definition);
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag.with(
        div("id: " + definition.getId()), div("version: " + definition.getVersion()), br());
    formTag
        .withAction(controllers.admin.routes.QuestionController.update(definition.getId()).url())
        .with(submitButton("Update"));

    return formTag;
  }

  private ContainerTag buildQuestionForm(QuestionForm questionForm) {
    ContainerTag formTag = form().withMethod("POST");
    formTag
        .with(
            FieldWithLabel.createInput("questionName")
                .setLabelText("Name")
                .setPlaceholderText("The name displayed in the question builder")
                .setValue(questionForm.getQuestionName())
                .getContainer(),
            FieldWithLabel.createTextArea("questionDescription")
                .setLabelText("Description")
                .setPlaceholderText("The description displayed in the question builder")
                .setValue(questionForm.getQuestionDescription())
                .getContainer(),
            FieldWithLabel.createInput("questionPath")
                .setLabelText("Path")
                .setPlaceholderText("The path used to store question data")
                .setValue(questionForm.getQuestionPath().path())
                .getContainer(),
            FieldWithLabel.createTextArea("questionText")
                .setLabelText("Question text")
                .setPlaceholderText("The question text displayed to the applicant")
                .setValue(questionForm.getQuestionText())
                .getContainer(),
            FieldWithLabel.createTextArea("questionHelpText")
                .setLabelText("Question help text")
                .setPlaceholderText("The question help text displayed to the applicant")
                .setValue(questionForm.getQuestionText())
                .getContainer())
        .with(formQuestionTypeSelect(QuestionType.valueOf(questionForm.getQuestionType())));

    return formTag;
  }

  private DomContent formQuestionTypeSelect(QuestionType selectedType) {
    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(QuestionType.values())
            .map(item -> new SimpleEntry<String, String>(item.toString(), item.name()))
            .collect(ImmutableList.toImmutableList());

    return new SelectWithLabel("questionType")
        .setLabelText("Question type")
        .setOptions(options)
        .setValue(selectedType.name())
        .getContainer();
  }
}
