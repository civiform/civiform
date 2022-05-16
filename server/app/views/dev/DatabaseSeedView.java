package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.pre;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import controllers.dev.routes;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.style.Styles;

/**
 * Renders a page for a developer to seed the database. This is only available in non-prod
 * environments.
 */
public class DatabaseSeedView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final ObjectMapper objectMapper;

  @Inject
  public DatabaseSeedView(BaseHtmlLayout layout, ObjectMapper objectMapper) {
    this.layout = checkNotNull(layout);
    this.objectMapper = checkNotNull(objectMapper);
    this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  public Content render(
      Request request,
      ActiveAndDraftPrograms activeAndDraftPrograms,
      ImmutableList<QuestionDefinition> questionDefinitions,
      Optional<String> maybeFlash) {

    ImmutableList<ProgramDefinition> draftPrograms = activeAndDraftPrograms.getDraftPrograms();
    ImmutableList<ProgramDefinition> activePrograms = activeAndDraftPrograms.getActivePrograms();

    String prettyDraftPrograms = getPrettyJson(draftPrograms);
    String prettyActivePrograms = getPrettyJson(activePrograms);
    String prettyQuestions = getPrettyJson(questionDefinitions);

    String title = "Dev database seeder";

    DivTag content =
        div()
            .with(div(maybeFlash.orElse("")))
            .with(h1(title))
            .with(
                div()
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("Generate mock program"))
                            .withMethod("post")
                            .attr("action", routes.DatabaseSeedController.seed().url()))
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("clear", "Clear entire database (irreversible!)"))
                            .withMethod("post")
                            .attr("action", routes.DatabaseSeedController.clear().url())))
            .with(
                div()
                    .withClasses(Styles.GRID, Styles.GRID_COLS_2)
                    .with(div().with(h2("Current Draft Programs:")).with(pre(prettyDraftPrograms)))
                    .with(
                        div().with(h2("Current Active Programs:")).with(pre(prettyActivePrograms)))
                    .with(div().with(h2("Current Questions:")).with(pre(prettyQuestions))))
            .withClasses(Styles.PX_6, Styles.PY_6);

    HtmlBundle bundle = layout.getBundle().setTitle(title).addMainContent(content);
    return layout.render(bundle);
  }

  private <T> String getPrettyJson(ImmutableList<T> list) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
