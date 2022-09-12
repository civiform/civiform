package views;

import static j2html.TagCreator.link;
import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.specialized.LinkTag;
import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import repository.ResetPostgres;

public class BaseHtmlLayoutTest extends ResetPostgres {

  private BaseHtmlLayout layout;

  @Before
  public void setUp() {
    layout = instanceOf(BaseHtmlLayout.class);
  }

  @Test
  public void addsDefaultContent() {
    HtmlBundle bundle = layout.getBundle();
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");

    assertThat(content.body())
        .contains("<link href=\"/assets/stylesheets/tailwind.css\" rel=\"stylesheet\">");
    assertThat(content.body())
        .contains(
            "<script src=\"/assets/javascripts/main.js\" data-has-loaded=\"false\""
                + " type=\"text/javascript\"></script>");
    assertThat(content.body())
        .contains(
            "<script src=\"/assets/javascripts/radio.js\" data-has-loaded=\"false\""
                + " type=\"text/javascript\"></script>");

    assertThat(content.body()).contains("<main></main>");
  }

  @Test
  public void canAddContentBefore() {
    HtmlBundle bundle = new HtmlBundle();

    // Add stylesheet before default.
    LinkTag linkTag = link().withHref("moose.css").withRel("stylesheet");
    bundle.addStylesheets(linkTag);

    bundle = layout.getBundle(bundle);
    Content content = layout.render(bundle);

    assertThat(content.body()).contains("<!DOCTYPE html><html lang=\"en\">");
    assertThat(content.body())
        .contains(
            "<link href=\"moose.css\" rel=\"stylesheet\"><link"
                + " href=\"/assets/stylesheets/tailwind.css\" rel=\"stylesheet\">");
  }
}
