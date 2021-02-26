package views.dev;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.head;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.title;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import controllers.dev.routes;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public class DatabaseSeedView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final ObjectMapper objectMapper;

  @Inject
  public DatabaseSeedView(BaseHtmlLayout layout, ObjectMapper objectMapper) {
    this.layout = layout;
    this.objectMapper = objectMapper;
  }

  public Content render(
      Request request,
      ImmutableList<ProgramDefinition> programDefinitions,
      ImmutableList<QuestionDefinition> questionDefinitions,
      Optional<String> maybeFlash) {

    String prettyPrograms = getPrettyJson(programDefinitions);
    String prettyQuestions = getPrettyJson(questionDefinitions);

    return layout.htmlContent(
        head(title("Seed database")),
        body()
            .with(div(maybeFlash.orElse("")))
            .with(h1("Dev mode database manager"))
            .with(div().with(h2("Current Programs:")).with(pre(prettyPrograms)))
            .with(div().with(h2("Current Questions:")).with(pre(prettyQuestions)))
            .with(
                form()
                    .with(makeCsrfTokenInputTag(request))
                    .with(submitButton("Generate mock program"))
                    .withMethod("post")
                    .withAction(routes.DatabaseSeedController.seed().url()))
            .with(
                form()
                    .with(makeCsrfTokenInputTag(request))
                    .with(submitButton("Clear all programs"))
                    .withMethod("post")
                    .withAction(routes.DatabaseSeedController.clear().url()))
            .with(layout.tailwindStyles()));
  }

  private <T> String getPrettyJson(ImmutableList<T> list) {
    try {
      return
              objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
