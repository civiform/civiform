package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
import static j2html.TagCreator.form;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import services.question.TranslationNotFoundException;
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

    Optional<String> questionText = Optional.empty();
    Optional<String> questionHelpText = Optional.empty();

    if (optionalDefinition.isPresent()) { // Editing a question.
      QuestionDefinition definition = optionalDefinition.get();
      buttonText = "Update";
      formTag.withAction(
          controllers.admin.routes.QuestionController.edit(definition.getPath()).toString());
      formTag.with(
          label("id: " + definition.getId()),
          br(),
          label("version: " + definition.getVersion()),
          br(),
          br());
      try {
        questionText = Optional.of(definition.getQuestionText(Locale.ENGLISH));
      } catch (TranslationNotFoundException e) {
        questionText = Optional.of("Missing Text");
      }
      try {
        questionHelpText = Optional.of(definition.getQuestionHelpText(Locale.ENGLISH));
      } catch (TranslationNotFoundException e) {
        questionHelpText = Optional.of("Missing Text");
      }
    } else {
      buttonText = "Create";
      formTag.withAction(controllers.admin.routes.QuestionController.newOne().toString());
    }

    formTag
        .with(
            textInputWithLabel(
                "Name: ",
                "questionName",
                optionalDefinition.isPresent()
                    ? Optional.of(optionalDefinition.get().getName())
                    : Optional.empty()))
        .with(
            textInputWithLabel(
                "Description: ",
                "questionDescription",
                optionalDefinition.isPresent()
                    ? Optional.of(optionalDefinition.get().getDescription())
                    : Optional.empty()))
        .with(
            textInputWithLabel(
                "Path: ",
                "questionPath",
                optionalDefinition.isPresent()
                    ? Optional.of(optionalDefinition.get().getPath())
                    : Optional.empty()))
        .with(textAreaWithLabel("Question Text: ", "questionText", questionText))
        .with(textAreaWithLabel("Question Help Text: ", "questionHelpText", questionHelpText))
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
