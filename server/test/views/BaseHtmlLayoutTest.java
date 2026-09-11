package views;

import static j2html.TagCreator.link;
import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.SectionTag;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import repository.ResetPostgres;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.settings.SettingsManifest;

public class BaseHtmlLayoutTest extends ResetPostgres {

  private static final ImmutableMap<String, String> DEFAULT_CONFIG =
      ImmutableMap.of(
          "base_url", "http://localhost",
          "staging_hostname", "localhost",
          "civiform_image_tag", "image",
          "favicon_url", "favicon",
          "whitelabel_civic_entity_short_name", "TestCity");

  private BaseHtmlLayout layout;

  @Before
  public void setUp() {
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(DEFAULT_CONFIG)),
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class));
  }

  @Test
  public void addsDefaultContent() {
    HtmlBundle bundle = layout.getBundle(fakeRequestBuilder().cspNonce("my-nonce").build());
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");

    assertThat(content.body())
        .containsPattern(
            "<link href=\"/assets/stylesheets/[a-z0-9]+-tailwind.css\" rel=\"stylesheet\""
                + " nonce=\"my-nonce\">");
    assertThat(content.body())
        .containsPattern(
            "<script src=\"/assets/dist/[a-z0-9]+-applicant.bundle.js\""
                + " type=\"module\" nonce=\"my-nonce\"></script>");
    assertThat(content.body()).doesNotContain("googletagmanager");

    assertThat(content.body()).contains("<main></main>");
  }

  @Test
  public void addsGoogleAnalyticsWhenContainsId() {
    HashMap<String, String> config = new HashMap<>(DEFAULT_CONFIG);
    config.put("measurement_id", "abcdef");
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(config)),
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class));
    HtmlBundle bundle = layout.getBundle(fakeRequestBuilder().cspNonce("my-nonce").build());
    Content content = layout.render(bundle);

    assertThat(content.body())
        .contains(
            "<script src=\"https://www.googletagmanager.com/gtag/js?id=abcdef\""
                + " async type=\"text/javascript\" nonce=\"my-nonce\"></script>");
  }

  @Test
  public void canAddContentBefore() {
    HtmlBundle bundle = new HtmlBundle(fakeRequestBuilder().cspNonce("my-nonce").build());

    // Add stylesheet before default.
    LinkTag linkTag = link().withHref("moose.css").withRel("stylesheet");
    bundle.addStylesheets(linkTag);

    bundle = layout.getBundle(bundle);
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");
    assertThat(content.body())
        .containsPattern(
            "<link href=\"moose.css\" rel=\"stylesheet\" nonce=\"my-nonce\"><link"
                + " href=\"/assets/dist/[a-z0-9]+-uswds_css.min.css\" rel=\"stylesheet\""
                + " nonce=\"my-nonce\"><link href=\"/assets/stylesheets/[a-z0-9]+-tailwind.css\""
                + " rel=\"stylesheet\" nonce=\"my-nonce\">");
  }

  @Test
  public void withNoExplicitTitle() {
    Content content = layout.render(layout.getBundle(fakeRequest()));

    assertThat(content.body()).contains("<title>CiviForm</title>");
  }

  @Test
  public void withProvidedTitle() {
    Content content = layout.render(layout.getBundle(fakeRequest()).setTitle("A title"));

    assertThat(content.body()).contains("<title>A title — CiviForm</title>");
  }

  @Test
  public void getGovBanner_returnsBannerWithHeader() {
    SectionTag banner = layout.getGovBanner(Optional.empty());

    String header = String.format("<header class=\"%s", "usa-banner__header");
    assertThat(banner.render()).contains(header);
  }

  @Test
  public void getGovBanner_returnsBannerWithContentDiv() {
    SectionTag banner = layout.getGovBanner(Optional.empty());

    String contentDiv = String.format("<div class=\"%s", "usa-banner__content");
    assertThat(banner.render()).contains(contentDiv);
  }

  @Test
  public void getDemoBanner_returnsBannerWithTitle() {
    play.i18n.Messages messages = instanceOf(play.i18n.MessagesApi.class).preferred(fakeRequest());
    j2html.tags.specialized.DivTag banner = layout.getDemoBanner(fakeRequest(), messages);

    String rendered = banner.render();
    assertThat(rendered).contains("Demo: TestCity");
    assertThat(rendered).contains("Demo mode informational alert");
    assertThat(rendered).doesNotContain("Do not enter actual or personal data in this demo site.");
    assertThat(rendered).contains("usa-site-alert--slim");
    assertThat(rendered).contains("usa-site-alert--no-icon");
    assertThat(rendered).contains("cf-demo-banner");
    assertThat(rendered).doesNotContain("usa-alert__heading");
    assertThat(rendered).doesNotContain("usa-tag");
  }

  @Test
  public void getDemoBanner_withLearnMoreUrl_containsLink() {
    HashMap<String, String> config = new HashMap<>(DEFAULT_CONFIG);
    config.put("demo_banner_learn_more_url", "https://example.com/demo-info");
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(config)),
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class));

    play.i18n.Messages messages = instanceOf(play.i18n.MessagesApi.class).preferred(fakeRequest());
    j2html.tags.specialized.DivTag banner = layout.getDemoBanner(fakeRequest(), messages);

    String rendered = banner.render();
    assertThat(rendered).contains("Demo: TestCity");
    assertThat(rendered).contains("https://example.com/demo-info");
    assertThat(rendered).contains("here");
    assertThat(rendered).contains("You can learn more");
    assertThat(rendered).doesNotContain("Do not enter actual or personal data in this demo site.");
  }

  @Test
  public void getDemoBanner_withMoreThan5DaysRemaining_containsGreenTag() {
    java.time.LocalDate futureDate =
        java.time.LocalDate.now(java.time.ZoneId.systemDefault()).plusDays(7);
    HashMap<String, String> config = new HashMap<>(DEFAULT_CONFIG);
    config.put(
        "demo_banner_expiration_date",
        futureDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(config)),
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class));

    play.i18n.Messages messages = instanceOf(play.i18n.MessagesApi.class).preferred(fakeRequest());
    j2html.tags.specialized.DivTag banner = layout.getDemoBanner(fakeRequest(), messages);

    String rendered = banner.render();
    assertThat(rendered).contains("Demo: TestCity");
    assertThat(rendered).contains("usa-tag bg-green");
    assertThat(rendered).contains("7 days remaining");
  }

  @Test
  public void getDemoBanner_with5OrLessDaysRemaining_containsYellowTag() {
    java.time.LocalDate futureDate =
        java.time.LocalDate.now(java.time.ZoneId.systemDefault()).plusDays(3);
    HashMap<String, String> config = new HashMap<>(DEFAULT_CONFIG);
    config.put(
        "demo_banner_expiration_date",
        futureDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(config)),
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class));

    play.i18n.Messages messages = instanceOf(play.i18n.MessagesApi.class).preferred(fakeRequest());
    j2html.tags.specialized.DivTag banner = layout.getDemoBanner(fakeRequest(), messages);

    String rendered = banner.render();
    assertThat(rendered).contains("Demo: TestCity");
    assertThat(rendered).contains("usa-tag bg-yellow text-ink");
    assertThat(rendered).contains("3 days remaining");
  }

  @Test
  public void getDemoBanner_withPastExpirationDate_doesNotMentionExpiration() {
    java.time.LocalDate pastDate =
        java.time.LocalDate.now(java.time.ZoneId.systemDefault()).minusDays(2);
    HashMap<String, String> config = new HashMap<>(DEFAULT_CONFIG);
    config.put(
        "demo_banner_expiration_date",
        pastDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(config)),
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class));

    play.i18n.Messages messages = instanceOf(play.i18n.MessagesApi.class).preferred(fakeRequest());
    j2html.tags.specialized.DivTag banner = layout.getDemoBanner(fakeRequest(), messages);

    String rendered = banner.render();
    assertThat(rendered).contains("Demo: TestCity");
    assertThat(rendered).doesNotContain("usa-tag");
    assertThat(rendered).doesNotContain("days remaining");
  }

  @Test
  public void getDemoBanner_withInvalidExpirationDate_doesNotMentionExpiration() {
    HashMap<String, String> config = new HashMap<>(DEFAULT_CONFIG);
    config.put("demo_banner_expiration_date", "not-a-valid-date");
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(config)),
            instanceOf(DeploymentType.class),
            instanceOf(BundledAssetsFinder.class));

    play.i18n.Messages messages = instanceOf(play.i18n.MessagesApi.class).preferred(fakeRequest());
    j2html.tags.specialized.DivTag banner = layout.getDemoBanner(fakeRequest(), messages);

    String rendered = banner.render();
    assertThat(rendered).contains("Demo: TestCity");
    assertThat(rendered).doesNotContain("usa-tag");
    assertThat(rendered).doesNotContain("days remaining");
  }

  @Test
  public void getDemoBannerTagColorClass_returnsExpectedColor() {
    assertThat(BaseHtmlLayout.getDemoBannerTagColorClass(6)).isEqualTo("bg-green");
    assertThat(BaseHtmlLayout.getDemoBannerTagColorClass(5)).isEqualTo("bg-yellow text-ink");
    assertThat(BaseHtmlLayout.getDemoBannerTagColorClass(0)).isEqualTo("bg-yellow text-ink");
  }

  @Test
  public void getDemoBannerDaysRemaining_returnsExpectedValues() {
    play.i18n.Messages messages = instanceOf(play.i18n.MessagesApi.class).preferred(fakeRequest());
    java.time.LocalDate fixedToday = java.time.LocalDate.of(2026, 9, 1);

    // Future date
    SettingsManifest futureManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-09-06")));
    assertThat(
            BaseHtmlLayout.getDemoBannerDaysRemaining(
                futureManifest, fakeRequest(), messages, fixedToday))
        .contains("5 days remaining");

    // Same day (0 days)
    SettingsManifest todayManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-09-01")));
    assertThat(
            BaseHtmlLayout.getDemoBannerDaysRemaining(
                todayManifest, fakeRequest(), messages, fixedToday))
        .contains("0 days remaining");

    // Past date (negative)
    SettingsManifest pastManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026-08-31")));
    assertThat(
            BaseHtmlLayout.getDemoBannerDaysRemaining(
                pastManifest, fakeRequest(), messages, fixedToday))
        .isEmpty();

    // Invalid date format
    SettingsManifest invalidManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "2026/09/06")));
    assertThat(
            BaseHtmlLayout.getDemoBannerDaysRemaining(
                invalidManifest, fakeRequest(), messages, fixedToday))
        .isEmpty();

    // Blank date
    SettingsManifest blankManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of("demo_banner_expiration_date", "  ")));
    assertThat(
            BaseHtmlLayout.getDemoBannerDaysRemaining(
                blankManifest, fakeRequest(), messages, fixedToday))
        .isEmpty();
  }
}
