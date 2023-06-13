package views;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CiviFormMarkdownTest {

  @Test
  public void render() {
    var subject = new CiviFormMarkdown();

    var result = subject.render("# one\ntwo [three](http://example.com)");

    assertThat(result)
        .isEqualTo(
            "<h1>one</h1>\n"
                + "<p>two <a href=\"http://example.com\" class=\"text-blue-600\""
                + " target=\"_blank\">three</a></p>\n");
  }
}
