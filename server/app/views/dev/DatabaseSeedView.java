package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.section;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import controllers.dev.seeding.routes;
import durablejobs.DurableJobName;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.SectionTag;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.DeploymentType;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.SelectWithLabel;

/**
 * Renders a page for a developer to seed the database. This is only available in non-prod
 * environments.
 */
public class DatabaseSeedView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final ObjectMapper objectMapper;
  private final DeploymentType deploymentType;

  @Inject
  public DatabaseSeedView(
      BaseHtmlLayout layout, ObjectMapper objectMapper, DeploymentType deploymentType) {
    this.layout = checkNotNull(layout);
    this.objectMapper = checkNotNull(objectMapper);
    this.deploymentType = checkNotNull(deploymentType);
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
                    .withClasses("grid", "grid-cols-2")
                    .with(div().with(h2("Current draft programs:")).with(pre(prettyDraftPrograms)))
                    .with(
                        div().with(h2("Current active programs:")).with(pre(prettyActivePrograms)))
                    .with(div().with(h2("Current questions:")).with(pre(prettyQuestions))))
            .withClasses("px-6", "py-6");

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(content);
    return layout.render(bundle);
  }

  public Content render(Request request, Optional<String> maybeFlash) {

    String title = "Dev tools";

    DivTag content =
        div()
            .withClasses("w-fit", "p-4")
            .condWith(maybeFlash.isPresent(), div(maybeFlash.orElse("")))
            .with(h1(title).withClasses("text-3xl", "mb-4"))
            .with(
                div()
                    .withClasses("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                    .with(createHomeSection())
                    .with(createSeedSection(request))
                    .with(createDurableJobsSection(request))
                    .with(createCachingSection(request))
                    .with(createIconsSection())
                    .condWith(deploymentType.isDev(), createEmailSection())
                    .with(createAddressToolsSection()));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(content);
    return layout.render(bundle);
  }

  private SectionTag createHomeSection() {
    return section()
        .withClasses("flex", "gap-4", "md:col-span-2")
        .with(
            createLink("\uD83C\uDFE0 Home page", controllers.routes.HomeController.index().url()));
  }

  private SectionTag createSeedSection(Request request) {
    return section()
        .withClasses("flex", "flex-col", "gap-4", "border", "border-black", "p-4")
        .with(h2("Seed").withClass("text-2xl"))
        .with(p("Populate, view, or clear sample data"))
        .with(
            createForm(
                request,
                "sample-programs",
                "Seed sample programs and categories",
                routes.DevDatabaseSeedController.seedPrograms().url()))
        .with(
            createForm(
                request,
                "sample-questions",
                "Seed sample questions",
                routes.DevDatabaseSeedController.seedQuestions().url()))
        .with(
            createForm(
                request,
                "clear",
                "Clear entire database (irreversible!)",
                routes.DevDatabaseSeedController.clear().url(),
                true))
        .with(createLink("View seed data", routes.DevDatabaseSeedController.data().url()));
  }

  private SectionTag createCachingSection(Request request) {
    return section()
        .with(h2("Caching").withClass("text-2xl"))
        .with(p("Manage or clear cache"))
        .withClasses("flex", "flex-col", "gap-4", "border", "border-black", "p-4")
        .with(
            createForm(
                request,
                "clear-cache",
                "\uD83D\uDCB5 Clear cache",
                routes.DevDatabaseSeedController.clearCache().url()));
  }

  private SectionTag createDurableJobsSection(Request request) {
    ImmutableList<SelectWithLabel.OptionValue> options =
        ImmutableList.copyOf(DurableJobName.values()).stream()
            .map(
                val ->
                    SelectWithLabel.OptionValue.builder()
                        .setLabel(val.toString())
                        .setValue(val.toString())
                        .build())
            .collect(ImmutableList.toImmutableList());

    FormTag formTag =
        form()
            .with(makeCsrfTokenInputTag(request))
            .with(
                submitButton("run-durable-job", "Run job")
                    .withClasses("w-full")
                    .withId("run-durable-job-button"))
            .withMethod("post")
            .withAction(routes.DevDatabaseSeedController.runDurableJob().url())
            .with(
                new SelectWithLabel()
                    .setId("durable-job-select")
                    .setFieldName("durableJobSelect")
                    .setLabelText("Choose job to run once")
                    .setOptions(options)
                    .setValue(options.get(0).label())
                    .getSelectTag());
    return section()
        .with(h2("Durable Jobs").withClass("text-2xl"))
        .withClasses("flex", "flex-col", "gap-4", "border", "border-black", "p-4")
        .with(p("Manually run the selected job"))
        .with(formTag);
  }

  private SectionTag createIconsSection() {
    return section()
        .with(h2("Icons").withClass("text-2xl"))
        .with(p("See all the svg icons in CiviForm"))
        .withClasses("flex", "flex-col", "gap-4", "border", "border-black", "p-4")
        .with(
            createLink("View All SVG Icons", controllers.dev.routes.IconsController.index().url()));
  }

  private SectionTag createEmailSection() {
    return section()
        .with(h2("Localstack").withClass("text-2xl"))
        .with(p("Open S3 or SES endpoints on localstack"))
        .withClasses("flex", "flex-col", "gap-4", "border", "border-black", "p-4")
        .with(
            createLink("✉\uFE0F View SES Emails", "http://localhost.localstack.cloud:4566/_aws/ses")
                .withTarget("_blank"),
            createLink(
                    "\uD83D\uDCC1 S3 Private Bucket",
                    "http://civiform-local-s3.localhost.localstack.cloud:4566/civiform-local-s3")
                .withTarget("_blank"),
            createLink(
                    "\uD83D\uDCC1 S3 Public Bucket",
                    "http://civiform-local-s3.localhost.localstack.cloud:4566/civiform-local-s3-public")
                .withTarget("_blank"));
  }

  private SectionTag createAddressToolsSection() {
    return section()
        .with(h2("Address Tools").withClass("text-2xl"))
        .with(p("View address lookup and eligibility results"))
        .withClasses("flex", "flex-col", "gap-4", "border", "border-black", "p-4")
        .with(
            createLink(
                "\uD83D\uDDFA\uFE0F Go to address tools", // <-- Unicode is a map emoji
                controllers.dev.routes.AddressCheckerController.index().url()));
  }

  private FormTag createForm(Request request, String buttonId, String buttonText, String url) {
    return createForm(request, buttonId, buttonText, url, false);
  }

  private FormTag createForm(
      Request request, String buttonId, String buttonText, String url, Boolean danger) {

    return form()
        .with(makeCsrfTokenInputTag(request))
        .with(
            submitButton(buttonId, buttonText)
                .withClasses("w-full")
                .withCondClass(danger, "bg-red-600 hover:bg-red-700 w-full"))
        .withMethod("post")
        .withAction(url);
  }

  private ATag createLink(String buttonText, String url) {
    return a().withHref(url)
        .withText(buttonText)
        .withClasses(
            "inline-block",
            "min-w-48",
            "py-2",
            "text-center",
            "text-white",
            "rounded",
            "font-semibold",
            "bg-blue-600",
            "hover:bg-blue-700");
  }

  private <T> String getPrettyJson(ImmutableList<T> list) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
