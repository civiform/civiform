package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.header;
import static j2html.TagCreator.img;
import static j2html.TagCreator.link;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.section;
import static j2html.TagCreator.span;
import static j2html.TagCreator.strong;
import static j2html.TagCreator.title;
import static views.BaseHtmlView.getCsrfToken;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.HeaderTag;
import j2html.tags.specialized.ScriptTag;
import j2html.tags.specialized.SectionTag;
import j2html.tags.specialized.SpanTag;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.MessageKey;
import services.settings.SettingsManifest;
import views.components.Icons;
import views.components.SessionTimeoutModals;
import views.components.ToastMessage;
import views.shared.FeatureFlags;
import views.trustedintermediary.ApplicantLayout;

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

  private static final String CIVIFORM_TITLE = "CiviForm";
  private static final String BANNER_TEXT =
      "Do not enter actual or personal data in this demo site";
  private final BundledAssetsFinder bundledAssetsFinder;

  public final ViewUtils viewUtils;
  protected final SettingsManifest settingsManifest;
  private final Optional<String> measurementId;
  private final boolean isDevOrStaging;
  private final boolean addNoindexMetaTag;

  @Inject
  public BaseHtmlLayout(
      ViewUtils viewUtils,
      SettingsManifest settingsManifest,
      DeploymentType deploymentType,
      BundledAssetsFinder bundledAssetsFinder) {
    this.viewUtils = checkNotNull(viewUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.measurementId = settingsManifest.getMeasurementId();

    this.isDevOrStaging = checkNotNull(deploymentType).isDevOrStaging();
    this.addNoindexMetaTag = settingsManifest.getStagingAddNoindexMetaTag();

    civiformImageTag = settingsManifest.getCiviformImageTag().get();
    this.bundledAssetsFinder = checkNotNull(bundledAssetsFinder);
  }

  /** Creates a new {@link HtmlBundle} with default css, scripts, and toast messages. */
  public HtmlBundle getBundle(Http.RequestHeader request) {
    return getBundle(new HtmlBundle(request));
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
   * ApplicantLayout} instead.
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
    bundle.setBundledAssetsFinder(bundledAssetsFinder);
    bundle.addMetadata(
        meta().withName("viewport").withContent("width=device-width, initial-scale=1"));
    bundle.addMetadata(meta().withName("civiform-build-tag").withContent(civiformImageTag));
    bundle.addMetadata(
        meta()
            .withName("htmx-config")
            .withContent(
                "{\"inlineStyleNonce\":\"" + CspUtil.getNonce(bundle.getRequest()) + "\"}"));
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

    var featureFlagJson =
        FeatureFlags.fromSettingsManifest(settingsManifest, bundle.getRequest()).toJson();

    bundle.addHeadScripts(
        script()
            .with(rawHtml(featureFlagJson))
            .withType("application/json")
            .withId("feature-flags-data"));

    if (bundledAssetsFinder.useBundlerDevServer()) {
      bundle.addHeadScripts(
          script().withSrc(bundledAssetsFinder.viteClientUrl()).withType("module"));
    }

    bundle.addHeadScripts(
        script()
            .withSrc(bundledAssetsFinder.getUswdsJsInit())
            .withType("text/javascript")
            .attr("crossorigin"));

    // Add default stylesheets.
    bundle.addStylesheets(
        link().withHref(bundledAssetsFinder.getUswdsStylesheet()).withRel("stylesheet"));
    bundle.addStylesheets(
        link().withHref(bundledAssetsFinder.getTailwindStylesheet()).withRel("stylesheet"));
    bundle.addStylesheets(
        link().withHref(bundledAssetsFinder.getMapLibreGLStylesheet()).withRel("stylesheet"));

    // Add Google analytics scripts.
    measurementId
        .map(id -> getAnalyticsScripts(id).toArray(new ScriptTag[0]))
        .ifPresent(bundle::addFooterScripts);

    // Add the favicon link
    bundle.setFavicon(settingsManifest.getFaviconUrl().get());
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
    // Best practice: add ❤️ every time you touch this file :)
    bundle.addMetadata(meta().withName("thanks").withContent("Thank you Bion ❤️❤️❤️"));
    return bundle.render();
  }

  protected void addSessionTimeoutModals(HtmlBundle bundle, Messages messages) {
    if (settingsManifest.getSessionTimeoutEnabled()
        && bundle.getRequest() instanceof Http.Request) {
      // Add the session timeout modals to the bundle
      Http.Request request = (Http.Request) bundle.getRequest();
      String csrfToken = getCsrfToken(request);
      bundle.addUswdsModals(SessionTimeoutModals.render(messages, csrfToken));
    }
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
        String.format(
            """
            window.dataLayer = window.dataLayer || [];
            function gtag() {
              dataLayer.push(arguments);
            }
            gtag('js', new Date());
            gtag('config', '%s');\
            """,
            trackingTag);
    ScriptTag rawScript = script().with(rawHtml(googleAnalyticsCode)).withType("text/javascript");
    return new ImmutableList.Builder<ScriptTag>().add(scriptImport).add(rawScript).build();
  }

  /**
   * Creates the banner which indicates that CiviForm is an official government website. This banner
   * is at the top of every page. It is a USWDS component:
   * https://designsystem.digital.gov/components/banner/
   *
   * @return a html section tag
   */
  protected SectionTag getGovBanner(Optional<Messages> maybeMessages) {
    SpanTag lockIcon =
        new SpanTag()
            .withClass("icon-lock")
            .with(
                Icons.svg(Icons.LOCK)
                    .withClasses("inline", "align-baseline", "usa-banner__lock-image")
                    .attr("viewBox", "0 0 52 64")
                    .attr("role", "img")
                    .attr("aria-label", "Locked padlock icon")
                    .attr("focusable", false)
                    .with(title("Lock").withId("banner-lock-title-default")));

    HeaderTag bannerHeader =
        header()
            .withClass("usa-banner__header")
            .with(
                div()
                    .withClasses("usa-banner__inner", "ml-0", "pl-4")
                    .with(div().withClass("grid-col-auto"))
                    .with(
                        div()
                            .withClasses("grid-col-fill", "tablet:grid-col-auto")
                            .attr("aria-hidden", true)
                            .with(
                                p(maybeMessages.isPresent()
                                        ? maybeMessages
                                            .get()
                                            .at(MessageKey.BANNER_TITLE.getKeyName())
                                        : "This is an official government website.")
                                    .withClass("usa-banner__header-text"),
                                p(maybeMessages.isPresent()
                                        ? maybeMessages
                                            .get()
                                            .at(MessageKey.BANNER_LINK.getKeyName())
                                        : "Here's how you know")
                                    .withClass("usa-banner__header-action")))
                    .with(
                        button()
                            .withType("button")
                            .withClasses(
                                "bg-transparent",
                                "p-0",
                                "usa-accordion__button",
                                "usa-banner__button")
                            .attr("aria-expanded", false)
                            .attr("aria-controls", "gov-banner-default-default")
                            .with(
                                span(maybeMessages.isPresent()
                                        ? maybeMessages
                                            .get()
                                            .at(MessageKey.BANNER_LINK.getKeyName())
                                        : "Here's how you know")
                                    .withClass("usa-banner__button-text"))));

    DivTag bannerContent =
        div()
            .withClasses("usa-banner__content", "usa-accordion__content", "sm: ml-0", "sm:pl-4")
            .withId("gov-banner-default-default")
            .isHidden()
            .with(
                div()
                    .withClasses("grid-row", "grid-gap-lg")
                    .with(
                        div()
                            .withClasses("usa-banner__guidance", "tablet:grid-col-6")
                            .with(
                                img()
                                    .withClasses("usa-banner__icon", "usa-media-block__img")
                                    .withSrc(
                                        bundledAssetsFinder.path("Images/uswds/icon-dot-gov.svg"))
                                    .withAlt("")
                                    .attr("role", "img")
                                    .attr("aria-hidden", true),
                                div()
                                    .withClass("usa-media-block__body")
                                    .with(
                                        p().with(
                                                strong(
                                                    maybeMessages.isPresent()
                                                        ? maybeMessages
                                                            .get()
                                                            .at(
                                                                MessageKey
                                                                    .BANNER_GOV_WEBSITE_SECTION_HEADER
                                                                    .getKeyName())
                                                        : "Official websites use .gov"),
                                                br(),
                                                span(
                                                    maybeMessages.isPresent()
                                                        ? maybeMessages
                                                            .get()
                                                            .at(
                                                                MessageKey
                                                                    .BANNER_GOV_WEBSITE_SECTION_CONTENT
                                                                    .getKeyName())
                                                        : "Website addresses ending in .gov"
                                                            + " belong to official"
                                                            + " government organizations in"
                                                            + " the United States.")))))
                    .with(
                        div()
                            .withClasses("usa-banner__guidance", "tablet:grid-col-6")
                            .with(
                                img()
                                    .withClasses("usa-banner__icon", "usa-media-block__img")
                                    .withSrc(
                                        bundledAssetsFinder.path("Images/uswds/icon-https.svg"))
                                    .withAlt("")
                                    .attr("role", "img")
                                    .attr("aria-hidden", true),
                                div()
                                    .withClass("usa-media-block__body")
                                    .with(
                                        p().with(
                                                strong(
                                                    maybeMessages.isPresent()
                                                        ? maybeMessages
                                                            .get()
                                                            .at(
                                                                MessageKey
                                                                    .BANNER_HTTPS_SECTION_HEADER
                                                                    .getKeyName())
                                                        : "Secure .gov websites use HTTPS"),
                                                br(),
                                                rawHtml(
                                                    maybeMessages.isPresent()
                                                        ? maybeMessages
                                                            .get()
                                                            .at(
                                                                MessageKey
                                                                    .BANNER_HTTPS_SECTION_CONTENT
                                                                    .getKeyName(),
                                                                lockIcon)
                                                        : String.format(
                                                            "<div>A lock ( %s ) or https:// means"
                                                                + " you've safely connected to the"
                                                                + " .gov website. Only share"
                                                                + " sensitive information on"
                                                                + " official, secure"
                                                                + " websites.</div>",
                                                            lockIcon)))))));

    return section()
        .withClasses("usa-banner", "bg-gray-900")
        .attr(
            "aria-label",
            // i18n messages are only available on the applicant views
            maybeMessages.isPresent()
                ? maybeMessages.get().at(MessageKey.BANNER_TITLE.getKeyName())
                : "This is an official government website.")
        .with(div().withClass("usa-accordion").with(bannerHeader, bannerContent));
  }

  /**
   * Creates the demo banner which warns users that they are in a demo environment.
   *
   * @return a html div tag
   */
  public DivTag getDemoBanner(Http.RequestHeader request, Messages messages) {
    Optional<String> maybeLearnMoreUrl = settingsManifest.getDemoBannerLearnMoreUrl(request);
    String civicEntityShortName =
        settingsManifest.getWhitelabelCivicEntityShortName(request).orElse("");
    String title = messages.at("label.demoBanner.heading", civicEntityShortName);

    Optional<Long> maybeDaysRemaining =
        getDemoBannerDaysRemainingCount(
            settingsManifest, request, viewUtils.getDateConverter().getCurrentDateForZoneId());

    ImmutableList.Builder<DomContent> bodyElements = ImmutableList.builder();
    bodyElements.add(p(title).withClass("usa-alert__text"));
    if (maybeDaysRemaining.isPresent()) {
      long daysRemaining = maybeDaysRemaining.get();
      String daysRemainingText = messages.at("banner.demoBanner.daysRemaining", daysRemaining);
      String tagColorClass = getDemoBannerTagColorClass(daysRemaining);
      bodyElements.add(div(span(daysRemainingText).withClasses("usa-tag", tagColorClass)));
    }
    if (maybeLearnMoreUrl.isPresent() && !maybeLearnMoreUrl.get().isBlank()) {
      String learnMoreUrl = maybeLearnMoreUrl.get();
      ATag moreInfoLink =
          a(messages.at("banner.demoBanner.linkText"))
              .withHref(learnMoreUrl)
              .withClasses("usa-link");
      DomContent textContent =
          rawHtml(messages.at("banner.demoBanner.bodyWithLink", moreInfoLink.render()));
      bodyElements.add(p(textContent).withClass("usa-alert__text"));
    }

    return div(
        section(
                div(div(bodyElements.build().toArray(new DomContent[0]))
                        .withClass("usa-alert__body"))
                    .withClass("usa-alert"))
            .withClasses(
                "usa-site-alert",
                "usa-site-alert--slim",
                "usa-site-alert--no-icon",
                "cf-alert",
                "cf-demo-banner")
            .attr("role", "region")
            .attr("aria-label", messages.at("label.demoBanner.label")));
  }

  /**
   * Returns the demo banner days remaining count if an expiration date is set and valid.
   *
   * @param settingsManifest the settings manifest to get the expiration date from
   * @param request the current request
   * @param currentDate the current date to calculate remaining days against
   * @return remaining days count, or empty if missing, invalid, or negative
   */
  public static Optional<Long> getDemoBannerDaysRemainingCount(
      SettingsManifest settingsManifest, Http.RequestHeader request, LocalDate currentDate) {
    Optional<String> maybeExpirationDate = settingsManifest.getDemoBannerExpirationDate(request);
    if (maybeExpirationDate.isEmpty() || maybeExpirationDate.get().isBlank()) {
      return Optional.empty();
    }
    try {
      LocalDate expirationDate =
          LocalDate.parse(maybeExpirationDate.get().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      long daysRemaining = ChronoUnit.DAYS.between(currentDate, expirationDate);
      if (daysRemaining < 0) {
        return Optional.empty();
      }
      return Optional.of(daysRemaining);
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  public static Optional<Long> getDemoBannerDaysRemainingCount(
      SettingsManifest settingsManifest, Http.RequestHeader request, ZoneId zoneId) {
    return getDemoBannerDaysRemainingCount(settingsManifest, request, LocalDate.now(zoneId));
  }

  public static Optional<Long> getDemoBannerDaysRemainingCount(
      SettingsManifest settingsManifest, Http.RequestHeader request) {
    return getDemoBannerDaysRemainingCount(settingsManifest, request, ZoneId.systemDefault());
  }

  /**
   * Returns the CSS color classes for the demo banner expiration tag.
   *
   * @param daysRemaining the count of remaining days
   * @return "bg-green" if > 5 days remain, "bg-yellow text-ink" if 5 or less days remain
   */
  public static String getDemoBannerTagColorClass(long daysRemaining) {
    return daysRemaining > 5 ? "bg-green" : "bg-yellow text-ink";
  }

  /**
   * Returns the demo banner days remaining message if an expiration date is set and valid.
   *
   * @param settingsManifest the settings manifest to get the expiration date from
   * @param request the current request
   * @param messages the messages provider for localization
   * @param currentDate the current date to calculate remaining days against
   * @return formatted days remaining message, or empty if missing, invalid, or negative
   */
  public static Optional<String> getDemoBannerDaysRemaining(
      SettingsManifest settingsManifest,
      Http.RequestHeader request,
      Messages messages,
      LocalDate currentDate) {
    return getDemoBannerDaysRemainingCount(settingsManifest, request, currentDate)
        .map(days -> messages.at("banner.demoBanner.daysRemaining", days));
  }

  public static Optional<String> getDemoBannerDaysRemaining(
      SettingsManifest settingsManifest,
      Http.RequestHeader request,
      Messages messages,
      ZoneId zoneId) {
    return getDemoBannerDaysRemaining(settingsManifest, request, messages, LocalDate.now(zoneId));
  }

  public static Optional<String> getDemoBannerDaysRemaining(
      SettingsManifest settingsManifest, Http.RequestHeader request, Messages messages) {
    return getDemoBannerDaysRemaining(settingsManifest, request, messages, ZoneId.systemDefault());
  }
}
