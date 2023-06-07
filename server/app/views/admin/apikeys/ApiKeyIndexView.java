package views.admin.apikeys;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.table;
import static j2html.TagCreator.td;
import static j2html.TagCreator.text;
import static j2html.TagCreator.th;
import static j2html.TagCreator.tr;

import auth.ApiKeyGrants;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TableTag;
import java.util.StringJoiner;
import java.util.function.Function;
import models.ApiKey;
import modules.MainModule;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DateConverter;
import services.PaginationResult;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.Icons;
import views.style.ReferenceClasses;

/** Renders a page that lists the system's {@link models.ApiKey}s. */
public final class ApiKeyIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final DateConverter dateConverter;

  @Inject
  public ApiKeyIndexView(AdminLayoutFactory layoutFactory, DateConverter dateConverter) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.API_KEYS);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(
      Http.Request request,
      PaginationResult<ApiKey> apiKeyPaginationResult,
      ImmutableSet<String> allProgramNames) {
    String title = "API Keys";
    ButtonTag newKeyButton =
        ViewUtils.makeSvgTextButton("New API Key", Icons.PLUS)
            .withId("new-api-key-button")
            .withClasses(ButtonStyles.SOLID_BLUE_WITH_ICON);
    DivTag headerDiv =
        div()
            .withClasses("flex", "items-center", "place-content-between", "my-8")
            .with(
                h1(title),
                asRedirectElement(
                    newKeyButton, controllers.admin.routes.AdminApiKeysController.newOne().url()));

    DivTag contentDiv = div().withClasses("px-20").with(headerDiv);

    for (ApiKey apiKey : apiKeyPaginationResult.getPageContents()) {
      contentDiv.with(renderApiKey(request, apiKey, buildProgramSlugToName(allProgramNames)));
    }

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderApiKey(
      Http.Request request, ApiKey apiKey, ImmutableMap<String, String> programSlugToName) {
    String keyNameSlugified = MainModule.SLUGIFIER.slugify(apiKey.getName());

    DivTag statsDiv =
        div()
            .with(
                p("Created " + dateConverter.renderDateTime(apiKey.getCreateTime()))
                    .withClasses(ReferenceClasses.BT_DATE),
                p("Created by " + apiKey.getCreatedBy())
                    .withClasses(ReferenceClasses.BT_API_KEY_CREATED_BY),
                p(apiKey
                        .getLastCallIpAddress()
                        .map(ip -> "Last used by " + ip)
                        .orElse("Last used by N/A"))
                    .withId(keyNameSlugified + "-last-call-ip"),
                p("Call count: " + apiKey.getCallCount()).withId(keyNameSlugified + "-call-count"))
            .withClasses("text-xs");

    DivTag linksDiv = div().withClasses("flex");

    if (apiKey.isRetired()) {
      statsDiv.with(p("Retired " + dateConverter.formatRfc1123(apiKey.getRetiredTime().get())));
    } else {

      String onSubmitJs =
          "return confirm('Retiring the API key is permanent and will prevent"
              + " anyone from being able to call the API with the key. Are you"
              + " sure you want to retire "
              + apiKey.getName()
              + "?')";
      linksDiv.with(
          form(makeCsrfTokenInputTag(request))
              .withAction(controllers.admin.routes.AdminApiKeysController.retire(apiKey.id).url())
              .withMethod("POST")
              .withOnsubmit(onSubmitJs)
              .withId(String.format("retire-%s", keyNameSlugified))
              .with(
                  makeSvgTextButton("Retire key", Icons.DELETE)
                      .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON)));
    }

    var subnetJoiner = new StringJoiner(", ");
    apiKey.getSubnetSet().forEach(subnetJoiner::add);

    DivTag topRowDiv =
        div()
            .with(
                div(
                    div(
                        p("ID: " + apiKey.getKeyId())
                            .withClasses("text-gray-700", "text-sm")
                            .withClasses(ReferenceClasses.BT_API_KEY_ID),
                        p("Allowed subnets: " + subnetJoiner.toString())
                            .withClasses("text-gray-700", "text-sm"),
                        p("Expires on: " + dateConverter.renderDate(apiKey.getExpiration()))
                            .withClasses("text-gray-700", "text-sm", ReferenceClasses.BT_DATE))),
                statsDiv)
            .withClasses("flex", "place-content-between");

    TableTag grantsTable =
        table()
            .withClasses("table-auto", "w-2/3")
            .with(
                tr(
                    th("Program name").withClasses("text-left", "text-sm"),
                    th("Program slug").withClasses("text-left", "text-sm"),
                    th("Permission").withClasses("text-left", "text-sm")));

    apiKey
        .getGrants()
        .getProgramGrants()
        .forEach(
            (String programSlug, ApiKeyGrants.Permission permission) -> {
              grantsTable.with(
                  tr(
                      td(programSlugToName.get(programSlug)),
                      td(programSlug),
                      td(permission.name())));
            });

    DivTag bottomDiv =
        div(grantsTable, linksDiv).withClasses("flex", "place-content-between", "mt-4");

    DivTag content =
        div()
            .withClasses("border", "border-gray-300", "bg-white", "rounded", "p-4")
            .with(
                h2().with(
                        text(apiKey.getName()),
                        span(apiKey.isRetired() ? " retired" : " active")
                            .withClasses("text-gray-700", "text-sm"))
                    .withClasses("mb-2", ReferenceClasses.ADMIN_API_KEY_INDEX_ENTRY_NAME),
                topRowDiv,
                bottomDiv);

    return div(content).withClasses("w-full", "shadow-lg", "mb-6");
  }

  private ImmutableMap<String, String> buildProgramSlugToName(ImmutableSet<String> programNames) {
    return programNames.stream()
        .collect(ImmutableMap.toImmutableMap(MainModule.SLUGIFIER::slugify, Function.identity()));
  }
}
