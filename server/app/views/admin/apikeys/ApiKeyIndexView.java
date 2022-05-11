package views.admin.apikeys;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.*;

import auth.ApiKeyGrants;
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZoneIdKeyDeserializer;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import models.ApiKey;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DateConverter;
import services.PaginationResult;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.Styles;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Renders a page that lists the system's {@link models.ApiKey}s. */
public final class ApiKeyIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final DateConverter dateConverter;
  // private final Slugify slugifier = new Slugify();

  @Inject
  public ApiKeyIndexView(AdminLayout layout, DateConverter dateConverter) {
    this.layout = checkNotNull(layout);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(Http.Request request, PaginationResult<ApiKey> apiKeyPaginationResult) {
    String title = "API Keys";
    ContainerTag headerDiv =
        div()
            .withClasses(Styles.FLEX, Styles.PLACE_CONTENT_BETWEEN)
            .with(
                h1(title).withClasses(Styles.MY_4),
                new LinkElement()
                    .setHref(controllers.admin.routes.AdminApiKeysController.newOne().url())
                    .setId("new-api-key-button")
                    .setText("New API Key")
                    .asButton());

    ContainerTag contentDiv = div().withClasses(Styles.PX_20).with(headerDiv);

    for (ApiKey apiKey : apiKeyPaginationResult.getPageContents()) {
      contentDiv.with(renderApiKey(request, apiKey));
    }

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  private ContainerTag renderApiKey(Http.Request request, ApiKey apiKey) {
    ContainerTag statsDiv = div()
        .with(
            p("Created " + dateConverter.formatRfc1123(apiKey.getCreateTime())),
            p("Created by " + apiKey.getCreatedBy()),
            p(apiKey.getLastCallIpAddress().map(ip -> "Last used by " + ip).orElse("Last used by N/A")),
            p("Call count: " + apiKey.getCallCount())
        )
        .withClasses(Styles.TEXT_XS);

    ContainerTag linksDiv = div().withClasses(Styles.FLEX);

    if (apiKey.isRetired()) {
      statsDiv.with(
          p("Retired " + dateConverter.formatRfc1123(apiKey.getRetiredTime().get())),
          p("Retired by " + apiKey.getRetiredBy().get())
      );
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
              .asHiddenForm(request));
    }

    ContainerTag topRowDiv = div()
        .with(div(
                h2(apiKey.getName()),
                p("ID: " + apiKey.getKeyId())
                    .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_SM)),
            div(apiKey.getSubnet()),
            statsDiv
        )
        .withClasses(Styles.FLEX, Styles.PLACE_CONTENT_BETWEEN);

    ContainerTag grantsTable = table().with(tr(th("Program name"), th("Program slug")));

    apiKey.getGrants().getProgramGrants().forEach((String programSlug, ApiKeyGrants.Permission permission) -> {
      grantsTable.with(
          tr(
              td(programSlug),
              td(programSlug),
              td(permission.name())
          )
      );
    });

    ContainerTag content =
        div()
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4)
            .with(
                topRowDiv,
                grantsTable,
                linksDiv);

    return div(content).withClasses(Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }
}
