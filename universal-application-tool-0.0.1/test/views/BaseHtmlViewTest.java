package views;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import j2html.tags.Text;
import org.junit.Before;
import org.junit.Test;

public class BaseHtmlViewTest {

  private TestImpl testImpl;

  @Before
  public void setUp() {
    testImpl = new TestImpl();
  }

  @Test
  public void submitButton_rendersAFormSubmitButton() {
    Tag result = testImpl.submitButton("text contents");

    assertThat(result.render()).isEqualTo("<button type=\"submit\">text contents</button>");
  }

  @Test
  public void urlsRenderCorrectly() {
    ImmutableList<DomContent> content =
        testImpl.createLinksAndEscapeText("hello google.com http://internet.website");
    assertThat(content).hasSize(4);
    assertThat(content.get(0).render()).isEqualTo(new Text("hello ").render());
    assertThat(content.get(1).render())
        .isEqualTo("<a href=\"http://google.com/\" class=\"opacity-75\">google.com</a>");
    assertThat(content.get(2).render()).isEqualTo(new Text(" ").render());
    assertThat(content.get(3).render())
        .isEqualTo(
            "<a href=\"http://internet.website/\""
                + " class=\"opacity-75\">http://internet.website</a>");
  }

  private static class TestImpl extends BaseHtmlView {}
}
