package views.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.endsWith;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import java.util.List;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import repository.ResetPostgres;

public class TextFormatterTest extends ResetPostgres {

  private void assertIsExternalUrlWithIcon(
      String actualValue, String expectedValue, String endsWith) {
    assertThat(actualValue).contains(expectedValue).endsWith(endsWith);
  }

  @Test
  public void urlsRenderCorrectly() {
    ImmutableList<DomContent> content =
        TextFormatter.formatText(
            "hello google.com http://internet.website https://secure.website",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false);
    String htmlContent = content.get(0).render();

    // URLs without protocols are not turned into links
    assertThat(htmlContent).contains("hello google.com ");

    // URLs with protocols are turned into links, the protocol is maintained and the SVG icon is
    // added
    List<String> contentArr = Splitter.on("</a>").splitToList(htmlContent);
    assertIsExternalUrlWithIcon(
        contentArr.get(0),
        "<a href=\"http://internet.website\" class=\"text-blue-900 font-bold opacity-75 underline"
            + " hover:opacity-100\" target=\"_blank\" aria-label=\"opens in a new tab\""
            + " rel=\"nofollow noopener noreferrer\">http://internet.website<svg",
        "</svg>");
    assertIsExternalUrlWithIcon(
        htmlContent,
        "<a href=\"https://secure.website\" class=\"text-blue-900 font-bold opacity-75 underline"
            + " hover:opacity-100\" target=\"_blank\" aria-label=\"opens in a new tab\""
            + " rel=\"nofollow noopener noreferrer\">https://secure.website<svg",
        "</svg></a></p>\n");
  }

  @Test
  public void textLinksRenderCorrectly() {
    ImmutableList<DomContent> content =
        TextFormatter.formatText(
            "[this is a link](https://www.google.com)",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false);
    String htmlContent = content.get(0).render();
    assertIsExternalUrlWithIcon(
        htmlContent,
        "<a href=\"https://www.google.com\" class=\"text-blue-900 font-bold opacity-75 underline"
            + " hover:opacity-100\" target=\"_blank\" aria-label=\"opens in a new tab\""
            + " rel=\"nofollow noopener noreferrer\">this is a link",
        "</svg></a></p>\n");
  }

  @Test
  public void rendersRequiredIndicator() {
    ImmutableList<DomContent> content =
        TextFormatter.formatText(
            "Enter your full legal name.",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true);

    assertThat(content.get(0).render())
        .isEqualTo(
            "<p>Enter your full legal name.<span class=\"text-red-600"
                + " font-semibold\">\u00a0*</span></p>\n");
  }

  @Test
  public void insertsRequiredIndicatorBeforeLists() {
    ImmutableList<DomContent> contentWithUnorderedList =
        TextFormatter.formatText(
            "Here is some text.\n" + "* list item one\n" + "* list item two",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true);
    String htmlContentWithUnorderedList = contentWithUnorderedList.get(0).render();
    assertThat(htmlContentWithUnorderedList)
        .isEqualTo(
            "<p>Here is some text.<span class=\"text-red-600 font-semibold\"> *</span></p>\n"
                + "<ul class=\"list-disc mx-8\"><li>list item one</li><li>list item"
                + " two</li></ul>\n");

    ImmutableList<DomContent> contentWithOrderedList =
        TextFormatter.formatText(
            "Here is some text.\n" + "1. list item one\n" + "2. list item two",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true);
    String htmlContentWithOrderedList = contentWithOrderedList.get(0).render();
    assertThat(htmlContentWithOrderedList)
        .isEqualTo(
            "<p>Here is some text.<span class=\"text-red-600 font-semibold\"> *</span></p>\n"
                + "<ol class=\"list-decimal mx-8\"><li>list item one</li><li>list item"
                + " two</li></ol>\n");
  }

  @Test
  public void insertsRequiredIndicatorAfterLists() {
    ImmutableList<DomContent> contentWithUnorderedList =
        TextFormatter.formatText(
            "- list item one\n" + "- list item two\n" + "- list item three",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true);
    String htmlContentWithUnorderedList = contentWithUnorderedList.get(0).render();
    assertThat(htmlContentWithUnorderedList)
        .isEqualTo(
            "<ul class=\"list-disc mx-8\"><li>list item one</li><li>list item two</li><li>list item"
                + " three<span class=\"text-red-600 font-semibold\"> *</span></li></ul>\n");

    ImmutableList<DomContent> contentWithOrderedList =
        TextFormatter.formatText(
            "1. list item one\n" + "2. list item two\n" + "3. list item three",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true);
    String htmlContentWithOrderedList = contentWithOrderedList.get(0).render();
    assertThat(htmlContentWithOrderedList)
        .isEqualTo(
            "<ol class=\"list-decimal mx-8\"><li>list item one</li><li>list item two</li><li>list"
                + " item three<span class=\"text-red-600 font-semibold\"> *</span></li></ol>\n");
  }

  @Test
  public void listRendersCorrectly() {
    String withList =
        "This is my list:\n" + "* cream cheese\n" + "* eggs\n" + "* sugar\n" + "* vanilla";
    ImmutableList<DomContent> content =
        TextFormatter.formatText(
            withList, /* preserveEmptyLines= */ false, /* addRequiredIndicator= */ false);
    String htmlContent = content.get(0).render();

    assertThat(htmlContent)
        .isEqualTo(
            "<p>This is my list:</p>\n"
                + "<ul class=\"list-disc mx-8\"><li>cream"
                + " cheese</li><li>eggs</li><li>sugar</li><li>vanilla</li></ul>\n");
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

    ImmutableList<DomContent> preservedBlanksContent =
        TextFormatter.formatText(
            withBlankLine, /* preserveEmptyLines= */ true, /* addRequiredIndicator= */ false);
    assertThat(preservedBlanksContent.get(0).render())
        .isEqualTo(
            "<p>This is the first line of content.<br /> </p>\n"
                + "<p>This is the second (or third) line of content.<br /> </p>\n"
                + "<p> </p>\n"
                + "<p>This is the third (or sixth) line of content.</p>\n");

    ImmutableList<DomContent> nonPreservedBlanksContent =
        TextFormatter.formatText(
            withBlankLine, /* preserveEmptyLines= */ false, /* addRequiredIndicator= */ false);
    assertThat(nonPreservedBlanksContent.get(0).render())
        .isEqualTo(
            "<p>This is the first line of content.</p>\n"
                + "<p>This is the second (or third) line of content.</p>\n"
                + "<p>This is the third (or sixth) line of content.</p>\n");
  }

  @Test
  public void appliesTextEmphasis() {
    String stringWithMarkdown =
        "# Hello!\nThis is a string with *italics* and **bold** and `inline code`";
    ImmutableList<DomContent> formattedText =
        TextFormatter.formatText(
            stringWithMarkdown, /* preserveEmptyLines= */ false, /* addRequiredIndicator= */ false);
    assertThat(formattedText.get(0).render())
        .isEqualTo(
            "<h2>Hello!</h2>\n"
                + "<p>This is a string with <em>italics</em> and <strong>bold</strong> and"
                + " <code>inline code</code></p>\n");
  }

  @Test
  public void removesScriptTags() {
    String stringWithScriptTag = "<script>alert('bad-time');</script>";
    ImmutableList<DomContent> formattedText =
        TextFormatter.formatText(
            stringWithScriptTag,
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false);
    assertThat(formattedText.get(0).render()).isEqualTo("\n");
  }

  @Test
  public void replacesH1Tags() {
    String stringWithH1Markdown =
        "# Header 1\n"
            + "should be changed to h2 but\n"
            + "## Header 2\n"
            + "and\n"
            + "### Header 3\n"
            + " should be allowed";
    ImmutableList<DomContent> formattedText =
        TextFormatter.formatText(stringWithH1Markdown, false, false);
    assertThat(formattedText.get(0).render())
        .isEqualTo(
            "<h2>Header 1</h2>\n"
                + "<p>should be changed to h2 but</p>\n"
                + "<h2>Header 2</h2>\n"
                + "<p>and</p>\n"
                + "<h3 class=\"text-lg\">Header 3</h3>\n"
                + "<p>should be allowed</p>\n");
  }

  @Test
  public void rejectedElementsAndAttributesAreLogged() {
    Logger logger = (Logger) LoggerFactory.getLogger(TextFormatter.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    String stringWithBadAttributesAndElements =
        "<script>console.log('uhoh')</script><div id=\"bad-id\"></div>";
    TextFormatter.formatText(stringWithBadAttributesAndElements, false, false);

    ImmutableList<ILoggingEvent> logsList = ImmutableList.copyOf(listAppender.list);
    assertThat(logsList.get(0).getMessage())
        .isEqualTo("HTML element: \"script\" was caught and discarded.");
    assertThat(logsList.get(1).getMessage())
        .isEqualTo("HTML attribute: \"id\" was caught and discarded.");
  }

  @Test
  public void formatTextWithAriaLabel_addsAriaLabel() {
    ImmutableList<DomContent> content =
        TextFormatter.formatTextWithAriaLabel(
            "[link](https://www.example.com)", false, false, "test aria label");

    assertThat(content.get(0).render()).contains("aria-label=\"test aria label\"");

    // Set the aria label back to the default for the other tests
    TextFormatter.resetAriaLabelToDefault();
  }

  @Test
  public void formatTextToSanitizedHTMLWithAriaLabel_addsAriaLabel() {
    String content =
        TextFormatter.formatTextToSanitizedHTMLWithAriaLabel(
            "[link](https://www.example.com)", false, false, "test aria label");

    assertThat(content).contains("aria-label=\"test aria label\"");

    // Set the aria label back to the default for the other tests
    TextFormatter.resetAriaLabelToDefault();
  }

  @Test
  public void formatTextToSanitizedHTMLWithAriaLabel_removesScriptTags() {
    String stringWithScriptTag = "<script>alert('bad-time');</script>";
    String formattedText =
        TextFormatter.formatTextToSanitizedHTMLWithAriaLabel(
            stringWithScriptTag,
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false,
            "test aria label");
    assertThat(formattedText).isEqualTo("\n");
  }

  @Test
  public void formatTextToSanitizedHTMLWithAriaLabel_appliesMarkdownFormatting() {
    String stringWithMarkdown =
        "# Hello!\nThis is a string with *italics* and **bold** and `inline code`";
    String formattedText =
        TextFormatter.formatTextToSanitizedHTMLWithAriaLabel(
            stringWithMarkdown,
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false,
            "aria ");
    assertThat(formattedText)
        .isEqualTo(
            "<h2>Hello!</h2>\n"
                + "<p>This is a string with <em>italics</em> and <strong>bold</strong> and"
                + " <code>inline code</code></p>\n");
  }
}
