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
import java.util.Optional;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import views.BaseHtmlView;
import views.admin.AdminLayout;

public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content renderNewQuestionForm(Request request) {
    Optional<String> maybeMessage = request.flash().get("message");
    return layout.render(
        body(
            div(maybeMessage.orElse("")),
            renderHeader("New Question"),
            buildNewQuestionForm(request).with(makeCsrfTokenInputTag(request))));
  }

  public Content renderEditQuestionForm(Request request, QuestionDefinition question) {
    Optional<String> maybeMessage = request.flash().get("message");
    return layout.render(
        body(
            div(maybeMessage.orElse("")),
            renderHeader("Edit Question"),
            buildEditQuestionForm(question).with(makeCsrfTokenInputTag(request))));
  }

  private ContainerTag buildNewQuestionForm(Request request) {
    QuestionForm questionForm = new QuestionForm();
    questionForm = fillDataFromRequest(request, questionForm);
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

  private QuestionForm fillDataFromRequest(Request request, QuestionForm questionForm) {
    Optional<String> questionName = request.flash().get("questionName");
    if (questionName.isPresent()) {
      questionForm.setQuestionName(questionName.get());
    }
    Optional<String> questionDescription = request.flash().get("questionDescription");
    if (questionDescription.isPresent()) {
      questionForm.setQuestionDescription(questionDescription.get());
    }
    Optional<String> questionPath = request.flash().get("questionPath");
    if (questionPath.isPresent()) {
      questionForm.setQuestionPath(questionPath.get());
    }
    Optional<String> questionType = request.flash().get("questionType");
    if (questionType.isPresent()) {
      questionForm.setQuestionType(questionType.get());
    }
    Optional<String> questionText = request.flash().get("questionText");
    if (questionText.isPresent()) {
      questionForm.setQuestionText(questionText.get());
    }
    Optional<String> questionHelpText = request.flash().get("questionHelpText");
    if (questionHelpText.isPresent()) {
      questionForm.setQuestionHelpText(questionHelpText.get());
    }
    return questionForm;
  }

  private ContainerTag buildQuestionForm(QuestionForm questionForm) {
    ContainerTag formTag = form().withMethod("POST");
    formTag
        .with(textInputWithLabel("Name: ", "questionName", questionForm.getQuestionName()))
        .with(
            textInputWithLabel(
                "Description: ", "questionDescription", questionForm.getQuestionDescription()))
        .with(textInputWithLabel("Path: ", "questionPath", questionForm.getQuestionPath()))
        .with(textAreaWithLabel("Question Text: ", "questionText", questionForm.getQuestionText()))
        .with(
            textAreaWithLabel(
                "Question Help Text: ", "questionHelpText", questionForm.getQuestionHelpText()))
        .with(formQuestionTypeSelect(QuestionType.valueOf(questionForm.getQuestionType())));

    return formTag;
  }

  private ImmutableList<DomContent> formQuestionTypeSelect(QuestionType selectedType) {
    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(QuestionType.values())
            .map(item -> new SimpleEntry<String, String>(item.toString(), item.name()))
            .collect(ImmutableList.toImmutableList());

    return formSelect("Question type: ", "questionType", options, selectedType.name());
  }
}
