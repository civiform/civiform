package views;

import static j2html.TagCreator.link;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import featureflags.FeatureFlags;
import j2html.tags.specialized.LinkTag;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import repository.ResetPostgres;

public class BaseHtmlLayoutTest extends ResetPostgres {

  private static final ImmutableMap<String, String> DEFAULT_CONFIG =
      ImmutableMap.of(
          "base_url", "http://localhost",
          "staging_hostname", "localhost",
          "civiform_image_tag", "image",
          "whitelabel.favicon_url", "favicon");

  private BaseHtmlLayout layout;

  @Before
  public void setUp() {
    layout =
        new BaseHtmlLayout(
            instanceOf(ViewUtils.class),
            ConfigFactory.parseMap(DEFAULT_CONFIG),
            instanceOf(FeatureFlags.class));
  }

  @Test
  public void addsDefaultContent() {
    HtmlBundle bundle = layout.getBundle();
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");

    assertThat(content.body())
        .containsPattern(
            "<link href=\"/assets/stylesheets/[a-z0-9]+-tailwind.css\" rel=\"stylesheet\">");
    assertThat(content.body())
        .containsPattern(
            "<script src=\"/assets/javascripts/[a-z0-9]+-applicant.bundle.js\""
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
            ConfigFactory.parseMap(config),
            instanceOf(FeatureFlags.class));
    HtmlBundle bundle = layout.getBundle();
    Content content = layout.render(bundle);

    assertThat(content.body())
        .contains(
            "<script src=\"https://www.googletagmanager.com/gtag/js?id=abcdef\""
                + " async type=\"text/javascript\"></script>");
  }

  @Test
  public void canAddContentBefore() {
    HtmlBundle bundle = new HtmlBundle(instanceOf(ViewUtils.class), /* enableJsBundles= */ true);

    // Add stylesheet before default.
    LinkTag linkTag = link().withHref("moose.css").withRel("stylesheet");
    bundle.addStylesheets(linkTag);

    bundle = layout.getBundle(bundle);
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");
    assertThat(content.body())
        .containsPattern(
            "<link href=\"moose.css\" rel=\"stylesheet\"><link"
                + " href=\"/assets/stylesheets/[a-z0-9]+-tailwind.css\" rel=\"stylesheet\">");
  }

  @Test
  public void withNoExplicitTitle() {
    Content content = layout.render(layout.getBundle());

    assertThat(content.body()).contains("<title>CiviForm</title>");
  }

  @Test
  public void withProvidedTitle() {
    Content content = layout.render(layout.getBundle().setTitle("A title"));

    assertThat(content.body()).contains("<title>A title — CiviForm</title>");
  }
}
