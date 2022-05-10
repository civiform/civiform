package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import j2html.tags.ContainerTag;
import javax.inject.Inject;
import models.ApiKey;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.style.Styles;

/** Renders a page that displays an API key's crentials after it's created. */
public final class ApiKeyCredentialsView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ApiKeyCredentialsView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(ApiKey apiKey, String credentials) {
    String title = "Created API key: " + apiKey.getName();

    ContainerTag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(title).withClasses(Styles.MY_4),
                h2("Credentials"),
                p(
                    "Please copy your API key and it store it somewhere secure. This is your only"
                        + " opportunity to copy the secret from CiviForm, if you \n"
                        + "refresh the page or navigate away you will not be able to recover the"
                        + " secret value and will need to create a new key instead."),
                p("API key: " + credentials).withClasses(Styles.MT_4));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);

    return layout.renderCentered(htmlBundle);
  }
}
