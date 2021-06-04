package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.Text;
import org.junit.Test;

public class TextFormatterTest {

  @Test
  public void urlsRenderCorrectly() {
    ImmutableList<DomContent> content =
        TextFormatter.createLinksAndEscapeText("hello google.com http://internet.website");
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

  @Test
  public void accordionRendersCorrectly() {

    String withList =
        "###Accordion Title\n"
            + ">These are a couple of lines of accordion content.\n"
            + ">\n"
            + ">Now I am going to go make a cheesecake.";
    ImmutableList<DomContent> content = TextFormatter.formatText(withList, false);
    assertThat(content).hasSize(1);

    String accordionContent = content.get(0).render();
    assertThat(accordionContent).startsWith("<div class=\"cf-accordion");
    assertThat(accordionContent).contains("These are a couple of lines of accordion content.");
    assertThat(accordionContent).contains("Now I am going to go make a cheesecake.");
  }

  @Test
  public void listRendersCorrectly() {
    String withList =
        "This is my list:\n" + "*cream cheese\n" + "*eggs\n" + "*sugar\n" + "*vanilla";
    ImmutableList<DomContent> content = TextFormatter.formatText(withList, false);
    assertThat(content).hasSize(2);

    // First item is just plain text.
    assertThat(content.get(0).render()).isEqualTo("<div>This is my list:</div>");
    // Second item is <ul>
    String listContent = content.get(1).render();
    assertThat(listContent).startsWith("<ul");
    assertThat(listContent).contains("<li>cream cheese</li>");
    assertThat(listContent).contains("<li>eggs</li>");
    assertThat(listContent).contains("<li>sugar</li>");
    assertThat(listContent).contains("<li>vanilla</li>");
    assertThat(listContent).endsWith("</ul>");
  }
}
