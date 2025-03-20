package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import j2html.tags.specialized.DivTag;
import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import repository.ResetPostgres;

public class HtmlBundleTest extends ResetPostgres {

  private ViewUtils viewUtils;

  @Before
  public void setUp() {
    viewUtils = instanceOf(ViewUtils.class);
  }

  @Test
  public void testSetTitle() {
    HtmlBundle bundle = new HtmlBundle(fakeRequest(), viewUtils);
    bundle.setTitle("My title").setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    assertThat(content.body()).contains("<title>My title</title>");
  }

  @Test
  public void testFavicon() {
    HtmlBundle bundle = new HtmlBundle(fakeRequest(), viewUtils);
    bundle.setFavicon("www.civiform.com/favicon").setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    assertThat(content.body()).contains("<link rel=\"icon\" href=\"www.civiform.com/favicon\">");
  }

  @Test
  public void testNoFavicon() {
    HtmlBundle bundle = new HtmlBundle(fakeRequest(), viewUtils);

    bundle.setJsBundle(JsBundle.APPLICANT);
    Content content = bundle.render();
    assertThat(content.body()).doesNotContain("<link rel=\"icon\"");
  }

  @Test
  public void emptyBundleRendersOutline() {
    HtmlBundle bundle =
        new HtmlBundle(fakeRequestBuilder().cspNonce("my-nonce").build(), viewUtils);

    bundle.setJsBundle(JsBundle.APPLICANT);
    Content content = bundle.render();
    assertThat(content.body())
        .containsPattern(
            "<body><header></header><main></main><div id=\"modal-container\" class=\"hidden fixed"
                + " h-screen w-screen z-20\"><div id=\"modal-glass-pane\" class=\"fixed h-screen"
                + " w-screen bg-gray-400 opacity-75\"></div></div><footer><script"
                + " src=\"/assets/dist/[a-z0-9]+-applicant.bundle.js\" type=\"text/javascript\""
                + " nonce=\"my-nonce\"></script><script"
                + " src=\"/assets/dist/[a-z0-9]+-uswds.bundle.js\" type=\"text/javascript\""
                + " nonce=\"my-nonce\"></script></footer></body>");
  }

  @Test
  public void rendersContentInOrder() {
    HtmlBundle bundle = new HtmlBundle(fakeRequest(), viewUtils);
    bundle.addMainContent(div("One")).addMainContent(div("Two")).setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    assertThat(content.body()).contains("<main><div>One</div><div>Two</div></main>");
  }

  @Test
  public void testUswdsModals() {
    HtmlBundle bundle = new HtmlBundle(fakeRequest(), viewUtils);

    DivTag modal =
        div()
            .withClass("usa-modal")
            .withId("test-modal")
            .with(
                div()
                    .withClass("usa-modal__content")
                    .with(
                        div()
                            .withClass("usa-modal__main")
                            .with(
                                h2("Test Modal").withClass("usa-modal__heading"),
                                p("Modal content"))));

    bundle.addUswdsModals(modal).setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    String html = content.body();

    assertThat(html).contains("<div id=\"uswds-modal-container\">");
    assertThat(html).contains("<div class=\"usa-modal\" id=\"test-modal\">");
    assertThat(html).contains("<div class=\"usa-modal__content\">");
    assertThat(html).contains("<div class=\"usa-modal__main\">");
    assertThat(html).contains("<h2 class=\"usa-modal__heading\">Test Modal</h2>");
    assertThat(html).contains("<p>Modal content</p>");
  }

  @Test
  public void testMultipleUswdsModals() {
    HtmlBundle bundle = new HtmlBundle(fakeRequest(), viewUtils);

    DivTag modal1 = div().withClass("usa-modal").withId("test-modal-1").with(p("First modal"));

    DivTag modal2 = div().withClass("usa-modal").withId("test-modal-2").with(p("Second modal"));

    bundle.addUswdsModals(modal1, modal2).setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    String html = content.body();

    assertThat(html).contains("<div id=\"uswds-modal-container\">");
    assertThat(html).contains("<div class=\"usa-modal\" id=\"test-modal-1\">");
    assertThat(html).contains("<div class=\"usa-modal\" id=\"test-modal-2\">");
    assertThat(html).contains("<p>First modal</p>");
    assertThat(html).contains("<p>Second modal</p>");
  }

  @Test
  public void testEmptyUswdsModalsContainer() {
    HtmlBundle bundle = new HtmlBundle(fakeRequest(), viewUtils);
    bundle.setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    String html = content.body();

    assertThat(html).doesNotContain("<div id=\"uswds-modal-container\"></div>");
  }
}
