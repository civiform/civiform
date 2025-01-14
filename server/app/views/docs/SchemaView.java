package views.docs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.label;
import static j2html.TagCreator.li;
import static j2html.TagCreator.link;
import static j2html.TagCreator.option;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.select;
import static j2html.TagCreator.style;
import static j2html.TagCreator.ul;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.docs.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.OptionTag;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import javax.inject.Inject;
import play.libs.F;
import play.mvc.Http;
import play.twirl.api.Content;
import services.openapi.OpenApiVersion;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.CspUtil;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.style.AdminStyles;
import views.style.BaseStyles;

public class SchemaView extends BaseHtmlView {
  public record Form(String programSlug, Optional<String> stage, Optional<String> openApiVersion) {}

  private final ProfileUtils profileUtils;
  private final BaseHtmlLayout unauthenticatedLayout;
  private final AdminLayout authenticatedLayout;

  @Inject
  public SchemaView(
      AdminLayoutFactory layoutFactory,
      ProfileUtils profileUtils,
      BaseHtmlLayout unauthenticatedLayout) {
    this.profileUtils = checkNotNull(profileUtils);
    this.unauthenticatedLayout = checkNotNull(unauthenticatedLayout);
    this.authenticatedLayout = layoutFactory.getLayout(NavPage.API_DOCS);
  }

  public Content render(
      Http.Request request,
      SchemaView.Form form,
      String apiUrl,
      ImmutableSet<String> allProgramSlugs) {
    String cspNonce = CspUtil.getNonce(request);
    BaseHtmlLayout layout =
        isAuthenticatedAdmin(request) ? authenticatedLayout : unauthenticatedLayout;

    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle("API schema")
            .addStylesheets(
                link()
                    .withRel("stylesheet")
                    .withHref("/public/swagger-ui/swagger-ui.css")
                    .attr("nonce", cspNonce))
            .addHeadScripts(
                script().withSrc("/public/swagger-ui/swagger-ui-bundle.js").attr("nonce", cspNonce))
            .addHeadScripts(
                script()
                    .withSrc("/public/swagger-ui/swagger-ui-standalone-preset.js")
                    .attr("nonce", cspNonce))
            .addMainContent(
                div(
                        h1("API schema").withClasses("ml-4"),
                        buildTabs(),
                        p().withClasses("ml-4", "my-4", "max-w-prose")
                            .withText(
                                "This page shows a visualization of the OpenAPI Schema. Both"
                                    + " version 2 (formerly Swagger 2) and version 3 are"
                                    + " available."),
                        p().withClasses("ml-4", "my-4", "max-w-prose")
                            .withText(
                                "We recommend only using version 3 unless there is a specific need"
                                    + " for the older format."),
                        div(buildForm(form, allProgramSlugs), buildOpenApiDisplay(apiUrl, cspNonce))
                            .withClasses("grid", "w-full"))
                    .withClasses("mx-6", "my-8", "w-full"));

    return layout.render(bundle);
  }

  /** Build the form for selecting the openapi schema to view */
  private FormTag buildForm(Form form, ImmutableSet<String> allProgramSlugs) {
    String url =
        routes.OpenApiSchemaController.getSchemaUIRedirect(
                Optional.empty(), Optional.empty(), Optional.empty())
            .url();

    String[] labelClasses = {"px-3", "bg-white", "text-lg", "py-2"};

    String[] selectClasses = {
      "px-3",
      "bg-white",
      "text-lg",
      "py-2",
      "block",
      "border",
      "border-gray-500",
      "rounded-lg",
      "max-w-64",
      "mr-4"
    };

    return form()
        .withId("form")
        .withAction(url)
        .with(
            label().withText("Program: ").withClasses(labelClasses).withFor("programSlug"),
            select()
                .with(getProgramSlugOptions(form, allProgramSlugs))
                .withClasses(selectClasses)
                .withId("programSlug")
                .withName("programSlug"),
            label().withText("Status: ").withClasses(labelClasses).withFor("stage"),
            select()
                .with(getStageOptions(form))
                .withId("stage")
                .withName("stage")
                .withClasses(selectClasses),
            label()
                .withText("OpenApi Version: ")
                .withClasses(labelClasses)
                .withFor("openApiVersion"),
            select()
                .with(getOpenApiOptions(form))
                .withId("openApiVersion")
                .withName("openApiVersion")
                .withClasses(selectClasses))
        .withClasses("flex", "flex-row", "mt-2", "ml-2");
  }

  private static ImmutableList<OptionTag> getProgramSlugOptions(
      Form form, ImmutableSet<String> allProgramSlugs) {
    return allProgramSlugs.stream()
        .sorted()
        .map(x -> option(x).withValue(x).withCondSelected(x.equalsIgnoreCase(form.programSlug)))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<OptionTag> getStageOptions(Form form) {
    return ImmutableList.of(new F.Tuple<>("Active", "active"), new F.Tuple<>("Draft", "draft"))
        .stream()
        .map(
            x ->
                option(x._1)
                    .withValue(x._2)
                    .withCondSelected(x._2.equalsIgnoreCase(form.stage.orElse(""))))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<OptionTag> getOpenApiOptions(Form form) {
    return Arrays.stream(OpenApiVersion.values())
        .sorted(Comparator.comparing(OpenApiVersion::toString))
        .map(
            x ->
                option(x.getVersionNumber())
                    .withValue(x.toString())
                    .withCondSelected(
                        x.toString().equalsIgnoreCase(form.openApiVersion.orElse(""))))
        .collect(ImmutableList.toImmutableList());
  }

  /** Build the bootstrap bits that will generate the swagger/openapi UI */
  private DivTag buildOpenApiDisplay(String apiUrl, String cspNonce) {
    String swaggerUiId = "swagger-ui";

    return div(
            style(rawHtml(".swagger-ui > .topbar { display: none; }"))
                .withType("text/css")
                .attr("nonce", cspNonce),
            div().withId(swaggerUiId),
            script(
                    rawHtml(
                        """
document.addEventListener('DOMContentLoaded', () => {
    SwaggerUIBundle({
        url: '%s',
        dom_id: '#%s',
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ],
        layout: 'StandaloneLayout',
        responseInterceptor: (response) => {
          if (response.status === 404) {
              console.log('404');
              const uiError = document.querySelector('.swagger-ui.swagger-container');
              const customErrorMessage = 'A program with this status could not be found.';
              uiError.innerHTML = `<div data-testid="ui-error" style="display: flex; justify-content: center; margin: 4rem; font-size: 1.6rem;">${customErrorMessage}</div>`;
          }
          return response;
        }
    });

    document.getElementById('programSlug')?.addEventListener('change', () => document.getElementById('form').submit());
    document.getElementById('stage')?.addEventListener('change', () => document.getElementById('form').submit());
    document.getElementById('openApiVersion')?.addEventListener('change', () => document.getElementById('form').submit());
});
"""
                            .formatted(apiUrl, swaggerUiId)
                            .stripIndent()))
                .attr("nonce", cspNonce)
                .withType("text/javascript"))
        .withClasses("justify-self-start");
  }

  private boolean isAuthenticatedAdmin(Http.Request request) {
    // CiviFormProfileFilter does not apply to API docs views, so there may be no profile
    Optional<CiviFormProfile> currentUserProfile = profileUtils.optionalCurrentUserProfile(request);
    return currentUserProfile.isPresent()
        && (currentUserProfile.get().isCiviFormAdmin()
            || currentUserProfile.get().isProgramAdmin());
  }

  private DivTag buildTabs() {
    return div(ul(
                li(a("API documentation").withHref(routes.ApiDocsController.index().url()))
                    .withClasses(AdminStyles.LINK_NOT_SELECTED),
                li(a("API Schema Viewer")
                        .withHref(
                            routes.OpenApiSchemaController.getSchemaUI(
                                    "", Optional.empty(), Optional.empty())
                                .url()))
                    .withClasses("border-b-2", AdminStyles.LINK_SELECTED))
            .withClasses("flex", "gap-4"))
        .withClasses("my-4", BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT);
  }
}
