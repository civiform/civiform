package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
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
import views.style.Styles;

public class DatabaseSeedView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final ObjectMapper objectMapper;

  @Inject
  public DatabaseSeedView(BaseHtmlLayout layout, ObjectMapper objectMapper) {
    this.layout = checkNotNull(layout);
    this.objectMapper = checkNotNull(objectMapper);
  }

  public Content render(
      Request request,
      ImmutableList<ProgramDefinition> programDefinitions,
      ImmutableList<QuestionDefinition> questionDefinitions,
      Optional<String> maybeFlash) {

    String prettyPrograms = getPrettyJson(programDefinitions);
    String prettyQuestions = getPrettyJson(questionDefinitions);

    return layout.htmlContent(
        head(title("Dev Database Seeder"), layout.tailwindStyles()),
        body()
            .with(div(maybeFlash.orElse("")))
            .with(h1("Dev Database Seeder"))
            .with(
                div()
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("Generate mock program"))
                            .withMethod("post")
                            .withAction(routes.DatabaseSeedController.seed().url()))
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("Clear all programs and questions"))
                            .withMethod("post")
                            .withAction(routes.DatabaseSeedController.clear().url())))
            .with(
                div()
                    .withClasses(Styles.GRID, Styles.GRID_COLS_2)
                    .with(div().with(h2("Current Programs:")).with(pre(prettyPrograms)))
                    .with(div().with(h2("Current Questions:")).with(pre(prettyQuestions))))
            .withClasses(Styles.PX_6, Styles.PY_6));
  }

  private <T> String getPrettyJson(ImmutableList<T> list) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
