package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.pre;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import controllers.dev.seeding.routes;
import j2html.tags.specialized.ATag;
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

  /**
   * Renders a page for a developer to view seeded data. This is only available in non-prod
   * environments.
   */
  public Content seedDataView(
      Request request,
      ActiveAndDraftPrograms activeAndDraftPrograms,
      ImmutableList<QuestionDefinition> questionDefinitions) {

    String title = "Dev database seed data";

    ImmutableList<ProgramDefinition> draftPrograms = activeAndDraftPrograms.getDraftPrograms();
    ImmutableList<ProgramDefinition> activePrograms = activeAndDraftPrograms.getActivePrograms();

    String prettyDraftPrograms = getPrettyJson(draftPrograms);
    String prettyActivePrograms = getPrettyJson(activePrograms);
    String prettyQuestions = getPrettyJson(questionDefinitions);

    ATag indexLinkTag =
        a().withHref(routes.DevDatabaseSeedController.index().url())
            .withId("index")
            .with(submitButton("index", "Go to dev database seeder page"));

    DivTag content =
        div()
            .with(h1(title))
            .with(indexLinkTag)
            .with(
                div()
                    .with(makeCsrfTokenInputTag(request))
                    .withClasses("grid", "grid-cols-2")
                    .with(div().with(h2("Current Draft Programs:")).with(pre(prettyDraftPrograms)))
                    .with(
                        div().with(h2("Current Active Programs:")).with(pre(prettyActivePrograms)))
                    .with(div().with(h2("Current Questions:")).with(pre(prettyQuestions))))
            .withClasses("px-6", "py-6");

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(content);
    return layout.render(bundle);
  }

  public Content render(Request request, Optional<String> maybeFlash) {

    String title = "Dev database seeder";

    ATag datalinkTag =
        a().withHref(routes.DevDatabaseSeedController.data().url())
            .with(submitButton("data", "Go to seed data page"));

    DivTag content =
        div()
            .with(div(maybeFlash.orElse("")))
            .with(h1(title))
            .with(
                div()
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("sample-programs", "Seed sample programs"))
                            .withMethod("post")
                            .withAction(routes.DevDatabaseSeedController.seedPrograms().url()))
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("sample-questions", "Seed sample questions"))
                            .withMethod("post")
                            .withAction(routes.DevDatabaseSeedController.seedQuestions().url()))
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("clear", "Clear entire database (irreversible!)"))
                            .withMethod("post")
                            .withAction(routes.DevDatabaseSeedController.clear().url()))
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("clear", "Clear cache"))
                            .withMethod("post")
                            .withAction(routes.DevDatabaseSeedController.clearCache().url()))
                    .with(
                        form()
                            .with(makeCsrfTokenInputTag(request))
                            .with(submitButton("index", "Go to index page"))
                            .withMethod("get")
                            .withAction(controllers.routes.HomeController.index().url()))
                    .with(datalinkTag));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(content);
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
