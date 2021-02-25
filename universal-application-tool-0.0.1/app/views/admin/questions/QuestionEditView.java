package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
import static j2html.TagCreator.form;
import static j2html.TagCreator.label;

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
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class QuestionEditView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public QuestionEditView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request, Optional<QuestionDefinition> question) {
    String headerText = question.isPresent() ? "Edit Question" : "New Question";
    return layout.htmlContent(
        body(
            renderHeader(headerText),
            buildQuestionForm(question).with(makeCsrfTokenInputTag(request))));
  }

  private ContainerTag buildQuestionForm(Optional<QuestionDefinition> optionalDefinition) {
    String buttonText = "";

    ContainerTag formTag = form().withMethod("POST");

    QuestionForm form = new QuestionForm(optionalDefinition);

    if (optionalDefinition.isPresent()) { // Editing a question.
      QuestionDefinition definition = optionalDefinition.get();
      buttonText = "Update";
      formTag.withAction(
          controllers.admin.routes.QuestionController.update(definition.getId()).url());
      formTag.with(
          label("id: " + definition.getId()),
          br(),
          label("version: " + definition.getVersion()),
          br(),
          br());
    } else {
      buttonText = "Create";
      formTag.withAction(controllers.admin.routes.QuestionController.create().url());
    }

    formTag
        .with(textInputWithLabel("Name: ", "questionName", form.getQuestionName()))
        .with(
            textInputWithLabel(
                "Description: ", "questionDescription", form.getQuestionDescription()))
        .with(textInputWithLabel("Path: ", "questionPath", form.getQuestionPath()))
        .with(textAreaWithLabel("Question Text: ", "questionText", form.getQuestionText()))
        .with(
            textAreaWithLabel(
                "Question Help Text: ", "questionHelpText", form.getQuestionHelpText()))
        .with(
            formQuestionTypeSelect(
                optionalDefinition.isPresent()
                    ? optionalDefinition.get().getQuestionType()
                    : QuestionType.TEXT))
        .with(submitButton(buttonText));

    return formTag;
  }

  private ImmutableList<DomContent> formQuestionTypeSelect(QuestionType selectedType) {
    QuestionType[] questionTypes = QuestionType.values();
    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(questionTypes)
            .map(item -> new SimpleEntry<String, String>(item.toString(), item.name()))
            .collect(ImmutableList.toImmutableList());

    return formSelect("Question type: ", "questionType", options, selectedType.name());
  }
}
