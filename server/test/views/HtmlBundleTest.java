package views;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import play.twirl.api.Content;

public class HtmlBundleTest {

  @Test
  public void testSetTitle() {
    HtmlBundle bundle = new HtmlBundle();
    bundle.setTitle("My title");

    Content content = bundle.render();
    assertThat(content.body()).contains("<title>My title</title>");
  }

  @Test
  public void testFavicon() {
    HtmlBundle bundle = new HtmlBundle();
    bundle.setFavicon("www.civiform.com/favicon");

    Content content = bundle.render();
    assertThat(content.body()).contains("<link rel=\"icon\" href=\"www.civiform.com/favicon\">");
  }

  @Test
  public void testNoFavicon() {
    HtmlBundle bundle = new HtmlBundle();

    Content content = bundle.render();
    assertThat(content.body()).doesNotContain("<link rel=\"icon\"");
  }

  @Test
  public void emptyBundleRendersOutline() {
    HtmlBundle bundle = new HtmlBundle();

    Content content = bundle.render();
    assertThat(content.body())
        .contains(
            "<body><header></header><main></main><div id=\"modal-container\" class=\"hidden fixed"
                + " h-screen w-screen\"><div id=\"modal-glass-pane\" class=\"fixed h-screen"
                + " w-screen bg-gray-400 opacity-75\"></div></div><footer></footer></body>");
  }

  @Test
  public void rendersContentInOrder() {
    HtmlBundle bundle = new HtmlBundle();
    bundle.addMainContent(div("One"));
    bundle.addMainContent(div("Two"));

    Content content = bundle.render();
    assertThat(content.body()).contains("<main><div>One</div><div>Two</div></main>");
  }
}
