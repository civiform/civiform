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
      Optional<String> maybeFlash) {

    String flash = "";
    if (maybeFlash.isPresent()) flash = maybeFlash.get();

    String prettyPrograms;
    try {
      prettyPrograms =
              objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(programDefinitions);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return layout.htmlContent(
        head(title("Seed database")),
        body()
            .with(div(flash))
            .with(h1("Dev mode database manager"))
            .with(div().with(h2("Current Programs:")).with(pre(prettyPrograms)))
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
}
