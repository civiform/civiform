package views.admin.apikeys;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
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
import j2html.tags.ContainerTag;
import java.util.function.Function;
import models.ApiKey;
import modules.MainModule;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DateConverter;
import services.PaginationResult;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.Styles;

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
    ContainerTag headerDiv =
        div()
            .withClasses(Styles.FLEX, Styles.PLACE_CONTENT_BETWEEN, Styles.MY_8)
            .with(
                h1(title).withClasses(Styles.MY_4),
                new LinkElement()
                    .setHref(controllers.admin.routes.AdminApiKeysController.newOne().url())
                    .setId("new-api-key-button")
                    .setText("New API Key")
                    .asButton());

    ContainerTag contentDiv = div().withClasses(Styles.PX_20).with(headerDiv);

    for (ApiKey apiKey : apiKeyPaginationResult.getPageContents()) {
      contentDiv.with(renderApiKey(request, apiKey, buildProgramSlugToName(allProgramNames)));
    }

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  private ContainerTag renderApiKey(
      Http.Request request, ApiKey apiKey, ImmutableMap<String, String> programSlugToName) {
    String keyNameSlugified = MainModule.SLUGIFIER.slugify(apiKey.getName());

    ContainerTag statsDiv =
        div()
            .with(
                p("Created " + dateConverter.formatRfc1123(apiKey.getCreateTime())),
                p("Created by " + apiKey.getCreatedBy()),
                p(apiKey
                        .getLastCallIpAddress()
                        .map(ip -> "Last used by " + ip)
                        .orElse("Last used by N/A"))
                    .withId(keyNameSlugified + "-last-call-ip"),
                p("Call count: " + apiKey.getCallCount()).withId(keyNameSlugified + "-call-count"))
            .withClasses(Styles.TEXT_XS);

    ContainerTag linksDiv = div().withClasses(Styles.FLEX);

    if (apiKey.isRetired()) {
      statsDiv.with(p("Retired " + dateConverter.formatRfc1123(apiKey.getRetiredTime().get())));
    } else {
      linksDiv.with(
          new LinkElement()
              .setHref(controllers.admin.routes.AdminApiKeysController.retire(apiKey.id).url())
              .setOnsubmit(
                  "return confirm('Retiring the API key is permanent and will prevent"
                      + " anyone from being able to call the API with the key. Are you"
                      + " sure you want to retire "
                      + apiKey.getName()
                      + "?')")
              .setText("Retire key")
              .setId(String.format("retire-%s", keyNameSlugified))
              .asHiddenForm(request));
    }

    ContainerTag topRowDiv =
        div()
            .with(
                div(
                    div(
                        p("ID: " + apiKey.getKeyId())
                            .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_SM),
                        p("Allowed subnet: " + apiKey.getSubnet())
                            .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_SM))),
                statsDiv)
            .withClasses(Styles.FLEX, Styles.PLACE_CONTENT_BETWEEN);

    ContainerTag grantsTable =
        table()
            .withClasses(Styles.TABLE_AUTO, Styles.W_2_3)
            .with(
                tr(
                    th("Program name").withClasses(Styles.TEXT_LEFT, Styles.TEXT_SM),
                    th("Program slug").withClasses(Styles.TEXT_LEFT, Styles.TEXT_SM),
                    th("Permission").withClasses(Styles.TEXT_LEFT, Styles.TEXT_SM)));

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

    ContainerTag bottomDiv =
        div(grantsTable, linksDiv)
            .withClasses(Styles.FLEX, Styles.PLACE_CONTENT_BETWEEN, Styles.MT_4);

    ContainerTag content =
        div()
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4)
            .with(
                h2().with(
                        text(apiKey.getName()),
                        span(apiKey.isRetired() ? " retired" : " active")
                            .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_SM))
                    .withClasses(Styles.MB_2, ReferenceClasses.ADMIN_API_KEY_INDEX_ENTRY_NAME),
                topRowDiv,
                bottomDiv);

    return div(content).withClasses(Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_6);
  }

  private ImmutableMap<String, String> buildProgramSlugToName(ImmutableSet<String> programNames) {
    return programNames.stream()
        .collect(ImmutableMap.toImmutableMap(MainModule.SLUGIFIER::slugify, Function.identity()));
  }
}
