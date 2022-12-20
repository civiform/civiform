package views;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

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
    HtmlBundle bundle = new HtmlBundle(viewUtils);
    bundle.setTitle("My title").setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    assertThat(content.body()).contains("<title>My title</title>");
  }

  @Test
  public void testFavicon() {
    HtmlBundle bundle = new HtmlBundle(viewUtils);
    bundle.setFavicon("www.civiform.com/favicon").setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    assertThat(content.body()).contains("<link rel=\"icon\" href=\"www.civiform.com/favicon\">");
  }

  @Test
  public void testNoFavicon() {
    HtmlBundle bundle = new HtmlBundle(viewUtils);

    bundle.setJsBundle(JsBundle.APPLICANT);
    Content content = bundle.render();
    assertThat(content.body()).doesNotContain("<link rel=\"icon\"");
  }

  @Test
  public void emptyBundleRendersOutline() {
    HtmlBundle bundle = new HtmlBundle(viewUtils);

    bundle.setJsBundle(JsBundle.APPLICANT);
    Content content = bundle.render();
    assertThat(content.body())
        .contains(
            "<body><header></header><main></main><div id=\"modal-container\" class=\"hidden fixed"
                + " h-screen w-screen z-20\"><div id=\"modal-glass-pane\" class=\"fixed h-screen"
                + " w-screen bg-gray-400 opacity-75\"></div></div><footer></footer></body>");
  }

  @Test
  public void rendersContentInOrder() {
    HtmlBundle bundle = new HtmlBundle(viewUtils);
    bundle.addMainContent(div("One")).addMainContent(div("Two")).setJsBundle(JsBundle.APPLICANT);

    Content content = bundle.render();
    assertThat(content.body()).contains("<main><div>One</div><div>Two</div></main>");
  }

  @Test
  public void rendersBundle() {
    HtmlBundle bundle = new HtmlBundle(viewUtils);
    Content content = bundle.setJsBundle(JsBundle.APPLICANT).render();
    assertThat(content.body()).contains("applicant.bundle.js");
  }
}
