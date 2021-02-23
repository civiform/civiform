package views.admin.questions;

import static j2html.TagCreator.a;
import static j2html.TagCreator.span;
import static j2html.TagCreator.td;

import j2html.tags.Tag;
import java.util.Locale;
import services.question.QuestionDefinition;
import services.question.TranslationNotFoundException;

enum QuestionRow {
  ACTIONS("Actions"),
  DESCRIPTION("Description"),
  ID("Id"),
  NAME("Name"),
  PATH("Path"),
  QUESTION_HELP_TEXT("Question Help Text"),
  QUESTION_TEXT("Question Text"),
  QUESTION_TYPE("Question Type"),
  VERSION("Version");

  private String headerText;

  QuestionRow(String headerText) {
    this.headerText = headerText;
  }

  public String getHeaderText() {
    return headerText;
  }

  public Tag getCellValue(QuestionDefinition definition) {
    switch (this) {
      case PATH:
        return td(definition.getPath());
      case ID:
        return td("" + definition.getId());
      case VERSION:
        return td("" + definition.getVersion());
      case NAME:
        return td(definition.getName());
      case DESCRIPTION:
        return td(definition.getDescription());
      case QUESTION_TEXT:
        try {
          return td(definition.getQuestionText(Locale.ENGLISH));
        } catch (TranslationNotFoundException e) {
          return td("");
        }
      case QUESTION_HELP_TEXT:
        try {
          return td(definition.getQuestionHelpText(Locale.ENGLISH));
        } catch (TranslationNotFoundException e) {
          return td("");
        }
      case QUESTION_TYPE:
        return td(definition.getQuestionType().toString());
      case ACTIONS:
        return td().with(span("view"))
            .with(span(" | "))
            .with(a("edit").withHref("/admin/questions/edit/" + definition.getPath()));
      default:
        return td("");
    }
  }
}
