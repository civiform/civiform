package views;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.Tag;
import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import views.BaseHtmlView.HtmlResponseContent;

public class BaseHtmlViewTest {

  private TestImpl testImpl;

  @Before
  public void setUp() {
    testImpl = new TestImpl();
  }

  @Test
  public void htmlContent_wrapsArgsInHtmlResponseContent() {
    Content result = testImpl.htmlContent(div("hello"));

    assertThat(result).isInstanceOf(HtmlResponseContent.class);
    assertThat(result.body()).isEqualTo("<!DOCTYPE html><html><div>hello</div></html>");
  }

  @Test
  public void textField_rendersATestFieldWrappedInALabel() {
    Tag result = testImpl.textField("fieldName", "label text");

    assertThat(result.render())
        .isEqualTo(
            "<label for=\"fieldName\">label text<input type=\"text\" name=\"fieldName\""
                + " id=\"fieldName\"></label>");
  }

  @Test
  public void submitButton_rendersAFormSubmitButton() {
    Tag result = testImpl.submitButton("text contents");

    assertThat(result.render())
        .isEqualTo("<input type=\"submit\" id=\"submit\" value=\"text contents\">");
  }

  private static class TestImpl extends BaseHtmlView {}
}
