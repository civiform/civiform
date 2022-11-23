package views;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.specialized.ButtonTag;
import org.junit.Test;

public class BaseHtmlViewTest {

  @Test
  public void submitButton_rendersAFormSubmitButton() {
    ButtonTag result = BaseHtmlView.submitButton("text contents");

    assertThat(result.render()).isEqualTo("<button type=\"submit\">text contents</button>");
  }

  private static class TestImpl extends BaseHtmlView {}
}
