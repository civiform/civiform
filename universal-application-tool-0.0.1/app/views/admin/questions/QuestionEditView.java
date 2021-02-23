package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.Arrays;
import java.util.Locale;
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
            buildQuestionForm(question.orElse(null)).with(makeCsrfTokenInputTag(request))));
  }

  public ContainerTag buildQuestionForm(QuestionDefinition definition) {
    String buttonText = "";

    ContainerTag formTag = form().withMethod("POST");
    if (definition != null) { // Editing a question.
      buttonText = "Update";
      formTag.withAction("/admin/questions/update");
      formTag.with(
          label("id: " + definition.getId()),
          br(),
          label("path: " + definition.getPath()),
          br(),
          label("version: " + definition.getVersion()),
          br(),
          br());
    } else {
      buttonText = "Create";
      formTag.withAction("/admin/questions/write");
    }
    formTag.with(
        inputWithLabel(
            "Name: ",
            "questionName",
            Optional.ofNullable(definition == null ? null : definition.getName())));
    formTag.with(
        inputWithLabel(
            "Description: ",
            "questionDescription",
            Optional.ofNullable(definition == null ? null : definition.getDescription())));
    if (definition == null) {
      formTag.with(inputWithLabel("Path: ", "questionPath", Optional.empty()));
    }
    Optional<String> questionText = Optional.empty();
    Optional<String> questionHelpText = Optional.empty();
    try {
      if (definition != null) {
        questionText = Optional.of(definition.getQuestionText(Locale.ENGLISH));
        questionHelpText = Optional.of(definition.getQuestionHelpText(Locale.ENGLISH));
      }
    } catch (Exception e) {
      questionText = Optional.of("Error");
      questionHelpText = Optional.of("Error");
    }
    formTag.with(this.textAreaWithLabel("Question Text: ", "questionText", questionText));
    formTag.with(
        this.textAreaWithLabel("Question Help Text: ", "questionHelpText", questionHelpText));
    formTag.with(
        this.formQuestionTypeSelect(
            definition == null ? QuestionType.TEXT : definition.getQuestionType()));
    formTag.with(input().withType("submit").withValue(buttonText));
    return formTag;
  }

  public ImmutableList<DomContent> formQuestionTypeSelect(QuestionType selectedType) {
    QuestionType[] questionTypes = QuestionType.values();
    String[] labels =
        Arrays.stream(questionTypes).map(item -> item.toString()).toArray(String[]::new);
    String[] values = Arrays.stream(questionTypes).map(item -> item.name()).toArray(String[]::new);
    return this.formSelect("Question type: ", "questionType", labels, values, selectedType.name());
  }
}
