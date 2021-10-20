package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import j2html.tags.Tag;
import java.net.URI;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.components.ToastMessage;

/**
 * Base class for all layout classes.
 *
 * <p>A layout class should describe the DOM contents of the head, header, nav, and footer. It acts
 * on an HtmlBundle and returns {@link Content} for rendered page using the {@link
 * #render(HtmlBundle)} method.
 */
public class BaseHtmlLayout {

  private static final String TAILWIND_COMPILED_FILENAME = "tailwind";
  private static final String[] FOOTER_SCRIPTS = {
    "main", "accordion", "modal", "number", "radio", "toast"
  };
  private static final String BANNER_TEXT =
      "Do not enter actual or personal data in this demo site";

  public final ViewUtils viewUtils;
  private final String measurementId;
  private final String hostName;
  private final boolean isStaging;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils, Config configuration) {
    this.viewUtils = checkNotNull(viewUtils);
    this.measurementId = checkNotNull(configuration).getString("measurement_id");

    String baseUrl = checkNotNull(configuration).getString("base_url");
    this.hostName = URI.create(baseUrl).getHost();
    String stagingHostname = checkNotNull(configuration).getString("staging_hostname");
    this.isStaging = hostName.equals(stagingHostname);
  }

  /** Creates a new {@link HtmlBundle} with default css, scripts, and toast messages. */
  public HtmlBundle getBundle() {
    return getBundle(new HtmlBundle());
  }

  /**
   * The reason for two getBundle methods here is that order occasionally matters, but this method
   * should only be used if you know what you're doing.
   *
   * <p>Most of the time you'll want to use {@link admin.AdminLayout} or {@link
   * applicant.ApplicantLayout} instead.
   *
   * <pre>
   *  Example: If we want to add specific styles before the core tailwind styles we
   *  could use:
   *
   *   HtmlBundle bundle = new HtmlBundle().addStylesheets(someStylesheet);
   *   bundle = getBundle(bundle);
   * </pre>
   */
  public HtmlBundle getBundle(HtmlBundle bundle) {
    // Add basic page metadata.
    bundle.addMetadata(
        meta().attr("name", "viewport").attr("content", "width=device-width, initial-scale=1"));

    // Add the warning toast, only for staging
    if (isStaging) {
      ToastMessage privacyBanner =
          ToastMessage.error(BANNER_TEXT)
              .setId("warning-message")
              .setIgnorable(true)
              .setDuration(0);
      bundle.addToastMessages(privacyBanner);
    }

    // Add default stylesheets.
    bundle.addStylesheets(viewUtils.makeLocalCssTag(TAILWIND_COMPILED_FILENAME));

    // Add Google analytics scripts.
    bundle.addFooterScripts(getAnalyticsScripts(measurementId).toArray(new Tag[0]));

    // Add default scripts.
    for (String source : FOOTER_SCRIPTS) {
      bundle.addFooterScripts(viewUtils.makeLocalJsTag(source));
    }

    return bundle;
  }

  /**
   * Render should add any additional styles, scripts, and tags and then return the fully rendered
   * page.
   */
  public Content render(HtmlBundle bundle) {
    return bundle.render();
  }

  /** Creates Google Analytics scripts for the site. */
  private ImmutableList<Tag> getAnalyticsScripts(String trackingTag) {
    Tag scriptImport =
        script()
            .withSrc("https://www.googletagmanager.com/gtag/js?id=" + trackingTag)
            .attr("async", "true")
            .withType("text/javascript");
    String googleAnalyticsCode =
        "window.dataLayer = window.dataLayer || [];"
            + "\nfunction gtag() {"
            + "\n\tdataLayer.push(arguments);"
            + "\n}"
            + "\ngtag('js', new Date());"
            + "\ngtag('config', '%s');";
    Tag rawScript =
        script()
            .with(rawHtml(String.format(googleAnalyticsCode, trackingTag)))
            .withType("text/javascript");
    return new ImmutableList.Builder<Tag>().add(scriptImport).add(rawScript).build();
  }
}
