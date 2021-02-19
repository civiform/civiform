package views;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import repository.WithPostgresContainer;

public class BaseHtmlLayoutTest extends WithPostgresContainer {

  private BaseHtmlLayout layout;

  @Before
  public void setUp() {
    layout = app.injector().instanceOf(BaseHtmlLayout.class);
  }

  @Test
  public void htmlContent_wrapsArgsInHtmlResponseContent() {
    Content result = layout.htmlContent(div("hello"));

    assertThat(result).isInstanceOf(BaseHtmlLayout.HtmlResponseContent.class);
    assertThat(result.body()).isEqualTo("<!DOCTYPE html><html><div>hello</div></html>");
  }
}
