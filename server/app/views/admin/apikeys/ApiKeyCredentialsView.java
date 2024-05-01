package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.PTag;
import javax.inject.Inject;
import models.ApiKeyModel;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Icons;
import views.components.LinkElement;

/** Renders a page that displays an API key's credentials after it's created. */
public final class ApiKeyCredentialsView extends BaseHtmlView {
  private final AdminLayout layout;

  private static final String CREDENTIALS_DESCRIPTION =
      "Please copy your API credentials and store them somewhere secure. This is your only"
          + " opportunity to copy the secret from CiviForm, if you"
          + " refresh the page or navigate away you will not be able to recover the"
          + " secret value and will need to create a new key instead.";

  private static final PTag CREDENTIALS_USERNAME_PASSWORD_EXPLANATION =
      p(
          text(
              "The API credentials are available as both a single token (the \"API Secret Token\")"
                  + " or a username/password combination, depending on the needs of your API"
                  + " client. See the "),
          new LinkElement()
              .setHref("https://docs.civiform.us/it-manual/api/authentication")
              .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
              .setText("API documentation")
              .opensInNewTab()
              .asAnchorText(),
          text("for details."));

  @Inject
  public ApiKeyCredentialsView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.API_KEYS);
  }

  public Content render(
      Http.Request request,
      ApiKeyModel apiKey,
      String encodedCredentials,
      String keyId,
      String keySecret) {
    String title = "Created API key: " + apiKey.getName();

    DivTag contentDiv =
        div()
            .withClasses("px-20")
            .with(
                h1(title).withClasses("my-4"),
                h2("Credentials"),
                p(CREDENTIALS_DESCRIPTION).withClasses("my-4"),
                CREDENTIALS_USERNAME_PASSWORD_EXPLANATION.withClasses("my-4"),
                p(
                        text("API secret token: "),
                        span(encodedCredentials)
                            .withId("api-key-credentials")
                            .withClasses("font-mono"))
                    .withClasses("my-4"),
                p(text("API username: "), span(keyId).withClasses("font-mono")),
                p(text("API password: "), span(keySecret).withClasses("font-mono")));

    HtmlBundle htmlBundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);

    return layout.renderCentered(htmlBundle);
  }
}
