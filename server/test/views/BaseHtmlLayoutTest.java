package views;

import static j2html.TagCreator.link;
import static org.assertj.core.api.Assertions.assertThat;
import static support.CfTestHelpers.EMPTY_REQUEST;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import controllers.AssetsFinder;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.SectionTag;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import repository.ResetPostgres;
import services.DeploymentType;
import services.settings.SettingsManifest;

public class BaseHtmlLayoutTest extends ResetPostgres {

  private static final ImmutableMap<String, String> DEFAULT_CONFIG =
      ImmutableMap.of(
          "base_url", "http://localhost",
          "staging_hostname", "localhost",
          "civiform_image_tag", "image",
          "favicon_url", "favicon");

  private BaseHtmlLayout layout;

  @Before
  public void setUp() {
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            new SettingsManifest(ConfigFactory.parseMap(DEFAULT_CONFIG)),
            instanceOf(DeploymentType.class),
            instanceOf(AssetsFinder.class));
  }

  @Test
  public void addsDefaultContent() {
    HtmlBundle bundle = layout.getBundle(EMPTY_REQUEST);
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");

    assertThat(content.body())
        .containsPattern(
            "<link href=\"/assets/stylesheets/[a-z0-9]+-tailwind.css\" rel=\"stylesheet\">");
    assertThat(content.body())
        .containsPattern(
            "<script src=\"/assets/dist/[a-z0-9]+-applicant.bundle.js\""
                + " type=\"text/javascript\"></script>");
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
            instanceOf(AssetsFinder.class));
    HtmlBundle bundle = layout.getBundle(EMPTY_REQUEST);
    Content content = layout.render(bundle);

    assertThat(content.body())
        .contains(
            "<script src=\"https://www.googletagmanager.com/gtag/js?id=abcdef\""
                + " async type=\"text/javascript\"></script>");
  }

  @Test
  public void canAddContentBefore() {
    HtmlBundle bundle = new HtmlBundle(EMPTY_REQUEST, instanceOf(ViewUtils.class));

    // Add stylesheet before default.
    LinkTag linkTag = link().withHref("moose.css").withRel("stylesheet");
    bundle.addStylesheets(linkTag);

    bundle = layout.getBundle(bundle);
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");
    assertThat(content.body())
        .containsPattern(
            "<link href=\"moose.css\" rel=\"stylesheet\"><link"
                + " href=\"/assets/dist/[a-z0-9]+-uswds.min.css\""
                + " rel=\"stylesheet\"><link href=\"/assets/stylesheets/[a-z0-9]+-tailwind.css\""
                + " rel=\"stylesheet\">");
  }

  @Test
  public void withNoExplicitTitle() {
    Content content = layout.render(layout.getBundle(EMPTY_REQUEST));

    assertThat(content.body()).contains("<title>CiviForm</title>");
  }

  @Test
  public void withProvidedTitle() {
    Content content = layout.render(layout.getBundle(EMPTY_REQUEST).setTitle("A title"));

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
}
