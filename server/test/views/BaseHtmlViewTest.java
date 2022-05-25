package views;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.Tag;
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

  private static class TestImpl extends BaseHtmlView {}
}
