package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.components.ToastMessage;

/**
 * Base class for all layout classes.
 *
 * <p>A layout class should describe the DOM contents of the head, header, nav, and footer. It acts
 * on an HtmlBundle and returns a rendered page.
 */
public class BaseHtmlLayout {
  private static final String TAILWIND_COMPILED_FILENAME = "tailwind";
  private static final String[] FOOTER_SCRIPTS = {"main", "radio", "toast"};
  private static final String TRACKING_TAG_ID = "G-HXM0Y35TGE";
  private static final String BANNER_TEXT =
      "Do not enter actual or personal data in this demo site";

  public final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  /** Creates a new {@link HtmlBundle} with default css, scripts, and toast messages. */
  public HtmlBundle getBundle() {
    return getBundle(new HtmlBundle());
  }

  /**
   * The reason for two getBundle methods here is that order occasionally matters, but this method
   * should only be used if you know what you're doing.
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
    // Add the warning toast.
    ToastMessage privacyBanner =
        ToastMessage.error(BANNER_TEXT).setId("warning-message").setIgnorable(true).setDuration(0);
    bundle.addToastMessages(privacyBanner);

    // Add default stylesheets.
    bundle.addStylesheets(viewUtils.makeLocalCssTag(TAILWIND_COMPILED_FILENAME));

    // Add Google analytics scripts.
    bundle.addFooterScripts(getAnalyticsScripts(TRACKING_TAG_ID).toArray(new Tag[0]));

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
