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
        .isEqualTo("<a href=\"http://google.com/\" class=\"text-seattle-blue\">google.com</a>");
    assertThat(content.get(2).render()).isEqualTo(new Text(" ").render());
    assertThat(content.get(3).render())
        .isEqualTo(
            "<a href=\"http://internet.website/\""
                + " class=\"text-seattle-blue\">http://internet.website</a>");
  }

  @Test
  public void urlParserSkipsTrailingPunctuation() {
    ImmutableList<DomContent> content =
        TextFormatter.createLinksAndEscapeText(
            "Hello google.com, crawl (http://seattle.gov/); and http://mysite.com...!");

    assertThat(content).hasSize(7);
    assertThat(content.get(0).render()).isEqualTo(new Text("Hello ").render());
    assertThat(content.get(1).render())
        .isEqualTo("<a href=\"http://google.com/\" class=\"text-seattle-blue\">google.com</a>");
    assertThat(content.get(2).render()).isEqualTo(new Text(", crawl (").render());
    assertThat(content.get(3).render())
        .isEqualTo("<a href=\"http://seattle.gov/\" class=\"text-seattle-blue\">http://seattle.gov/</a>");
    assertThat(content.get(4).render()).isEqualTo(new Text("); and ").render());
    assertThat(content.get(5).render())
        .isEqualTo("<a href=\"http://mysite.com/\" class=\"text-seattle-blue\">http://mysite.com</a>");
    assertThat(content.get(6).render()).isEqualTo(new Text("...!").render());
  }

  @Test
  public void accordionRendersCorrectly() {

    String withList =
        "### Accordion Title\n"
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
        "This is my list:\n" + "* cream cheese\n" + "* eggs\n" + "* sugar\n" + "* vanilla";
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

  @Test
  public void rendersRecursively() {
    String text =
        "Cheesecake Recipe\n"
            + "### Ingredients\n"
            + ">You will need:\n"
            + ">* cream cheese\n"
            + ">* eggs\n"
            + ">* sugar\n"
            + ">* vanilla\n"
            + "### Directions\n"
            + "> View directions and the rest of the recipe at epicurious.com";
    ImmutableList<DomContent> content = TextFormatter.formatText(text, false);

    assertThat(content).hasSize(3);

    String[] contentStrings = content.stream().map(line -> line.render()).toArray(String[]::new);

    assertThat(contentStrings[0]).isEqualTo("<div>Cheesecake Recipe</div>");

    // Verify that we have an accordion.
    assertThat(contentStrings[1]).startsWith("<div class=\"cf-accordion");
    assertThat(contentStrings[1]).contains("Ingredients");

    // ...and that accordion contains a div.
    assertThat(contentStrings[1]).contains("<div>You will need:</div>");

    // ...and a list.
    assertThat(contentStrings[1])
        .contains(
            "<ul class=\"list-disc mx-8\">"
                + "<li>cream cheese</li><li>eggs</li><li>sugar</li><li>vanilla</li>"
                + "</ul>");

    // Verify that we have a second accordion.
    assertThat(contentStrings[2]).startsWith("<div class=\"cf-accordion");
    assertThat(contentStrings[2]).contains("Directions");

    // ...with text
    assertThat(contentStrings[2]).contains("View directions and the rest of the recipe at");

    // ...and a link.
    assertThat(contentStrings[2])
        .contains(
            "<a href=\"http://epicurious.com/\"" + " class=\"text-seattle-blue\">epicurious.com</a>");
  }

  @Test
  public void preservesLines() {
    String withBlankLine =
        "This is the first line of content.\n"
            + "\n"
            + "This is the second (or third) line of content.\n"
            + "\n"
            + "\n"
            + "This is the third (or sixth) line of content.";
    ImmutableList<DomContent> preservedBlanks = TextFormatter.formatText(withBlankLine, true);

    assertThat(preservedBlanks).hasSize(6);

    String[] preservedContent =
        preservedBlanks.stream().map(line -> line.render()).toArray(String[]::new);

    assertThat(preservedContent[0]).isEqualTo("<div>This is the first line of content.</div>");
    assertThat(preservedContent[1]).isEqualTo("<div class=\"h-6\"></div>");
    assertThat(preservedContent[2])
        .isEqualTo("<div>This is the second (or third) line of content.</div>");
    assertThat(preservedContent[3]).isEqualTo("<div class=\"h-6\"></div>");
    assertThat(preservedContent[4]).isEqualTo("<div class=\"h-6\"></div>");
    assertThat(preservedContent[5])
        .isEqualTo("<div>This is the third (or sixth) line of content.</div>");

    ImmutableList<DomContent> nonPresercedBlanks = TextFormatter.formatText(withBlankLine, false);
    assertThat(nonPresercedBlanks).hasSize(3);

    String[] nonPreservedContent =
        nonPresercedBlanks.stream().map(line -> line.render()).toArray(String[]::new);

    assertThat(nonPreservedContent[0]).isEqualTo("<div>This is the first line of content.</div>");
    assertThat(nonPreservedContent[1])
        .isEqualTo("<div>This is the second (or third) line of content.</div>");
    assertThat(nonPreservedContent[2])
        .isEqualTo("<div>This is the third (or sixth) line of content.</div>");
  }
}
