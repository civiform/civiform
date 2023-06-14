package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import javax.inject.Inject;
import play.twirl.api.Content;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.components.ToastMessage;

// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING BaseHtmlLayout

/**
 * Base class for all layout classes.
 *
 * <p>A layout class should describe the DOM contents of the head, header, nav, and footer. It acts
 * on an HtmlBundle and returns {@link Content} for rendered page using the {@link
 * #render(HtmlBundle)} method.
 */
public class BaseHtmlLayout {
  private final String civiformImageTag;
  private final String civiformFaviconUrl;

  private static final String CIVIFORM_TITLE = "CiviForm";
  private static final String TAILWIND_COMPILED_FILENAME = "tailwind";
  private static final String BANNER_TEXT =
      "Do not enter actual or personal data in this demo site";

  public final ViewUtils viewUtils;
  private final SettingsManifest settingsManifest;
  private final Optional<String> measurementId;
  private final boolean isDevOrStaging;
  private final boolean addNoindexMetaTag;

  @Inject
  public BaseHtmlLayout(
      ViewUtils viewUtils,
      Config configuration,
      SettingsManifest settingsManifest,
      DeploymentType deploymentType) {
    checkNotNull(configuration);
    this.viewUtils = checkNotNull(viewUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.measurementId =
        configuration.hasPath("measurement_id")
            ? Optional.of(configuration.getString("measurement_id"))
            : Optional.empty();

    this.isDevOrStaging = checkNotNull(deploymentType).isDevOrStaging();
    this.addNoindexMetaTag =
        this.isDevOrStaging && configuration.getBoolean("staging_add_noindex_meta_tag");

    civiformImageTag = configuration.getString("civiform_image_tag");
    civiformFaviconUrl = configuration.getString("favicon_url");
  }

  /** Creates a new {@link HtmlBundle} with default css, scripts, and toast messages. */
  public HtmlBundle getBundle() {
    return getBundle(new HtmlBundle(viewUtils));
  }

  /** Get the application feature flags. */
  public SettingsManifest getSettingsManifest() {
    return settingsManifest;
  }

  /**
   * The reason for two getBundle methods here is that order occasionally matters, but this method
   * should only be used if you know what you're doing.
   *
   * <p>Most of the time you'll want to use {@link views.admin.AdminLayout} or {@link
   * views.applicant.ApplicantLayout} instead.
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
        meta().withName("viewport").withContent("width=device-width, initial-scale=1"));
    bundle.addMetadata(meta().withName("civiform-build-tag").withContent(civiformImageTag));
    if (addNoindexMetaTag) {
      bundle.addMetadata(meta().withName("robots").withContent("noindex"));
    }

    // Add the warning toast, only for staging
    if (isDevOrStaging) {
      ToastMessage privacyBanner =
          ToastMessage.warning(BANNER_TEXT)
              .setId("warning-message")
              .setIgnorable(true)
              .setDuration(0);
      bundle.addToastMessages(privacyBanner);
    }

    // Add default stylesheets.
    bundle.addStylesheets(viewUtils.makeLocalCssTag(TAILWIND_COMPILED_FILENAME));

    // Add Google analytics scripts.
    measurementId
        .map(id -> getAnalyticsScripts(id).toArray(new ScriptTag[0]))
        .ifPresent(bundle::addFooterScripts);

    // Add the favicon link
    bundle.setFavicon(civiformFaviconUrl);
    bundle.setJsBundle(getJsBundle());

    return bundle;
  }

  /**
   * Render should add any additional styles, scripts, and tags and then return the fully rendered
   * page.
   */
  public Content render(HtmlBundle bundle) {
    String currentTitle = bundle.getTitle();
    if (Strings.isNullOrEmpty(currentTitle)) {
      bundle.setTitle(getTitleSuffix());
    } else {
      bundle.setTitle(String.format("%s — %s", currentTitle, getTitleSuffix()));
    }
    return bundle.render();
  }

  protected String getTitleSuffix() {
    return CIVIFORM_TITLE;
  }

  protected JsBundle getJsBundle() {
    return JsBundle.APPLICANT;
  }

  /** Creates Google Analytics scripts for the site. */
  private ImmutableList<ScriptTag> getAnalyticsScripts(String trackingTag) {
    ScriptTag scriptImport =
        script()
            .withSrc("https://www.googletagmanager.com/gtag/js?id=" + trackingTag)
            .isAsync()
            .withType("text/javascript");
    String googleAnalyticsCode =
        "window.dataLayer = window.dataLayer || [];"
            + "\nfunction gtag() {"
            + "\n\tdataLayer.push(arguments);"
            + "\n}"
            + "\ngtag('js', new Date());"
            + "\ngtag('config', '%s');";
    ScriptTag rawScript =
        script()
            .with(rawHtml(String.format(googleAnalyticsCode, trackingTag)))
            .withType("text/javascript");
    return new ImmutableList.Builder<ScriptTag>().add(scriptImport).add(rawScript).build();
  }
}
