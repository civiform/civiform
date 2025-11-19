package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import repository.ResetPostgres;

public class TextFormatterTest extends ResetPostgres {

  @Test
  public void urlsRenderCorrectly() {
    ImmutableList<DomContent> content =
        TextFormatter.formatTextForAdmins(
            "hello google.com http://internet.website https://secure.website");
    String htmlContent = content.get(0).render();

    // URLs without protocols are not turned into links
    assertThat(htmlContent).contains("hello google.com ");

    // URLs with protocols are turned into links, the protocol is maintained and aria-label is
    // added
    assertThat(htmlContent)
        .contains(
            "<a href=\"http://internet.website\" class=\"usa-link usa-link--external\""
                + " target=\"_blank\" aria-label=\"http://internet.website, opens in a new tab\""
                + " rel=\"nofollow noopener noreferrer\">http://internet.website</a>");
    assertThat(htmlContent)
        .contains(
            "<a href=\"https://secure.website\" class=\"usa-link usa-link--external\""
                + " target=\"_blank\" aria-label=\"https://secure.website, opens in a new tab\""
                + " rel=\"nofollow noopener noreferrer\">https://secure.website</a>");
  }

  @Test
  public void textLinksRenderCorrectly() {
    ImmutableList<DomContent> content =
        TextFormatter.formatTextForAdmins("[this is a link](https://www.google.com)");
    String htmlContent = content.get(0).render();
    assertThat(htmlContent)
        .contains(
            "<a href=\"https://www.google.com\" class=\"usa-link usa-link--external\""
                + " target=\"_blank\" aria-label=\"this is a link, opens in a new tab\""
                + " rel=\"nofollow noopener noreferrer\">this is a link</a>");
  }

  @Test
  public void rendersRequiredIndicator() {
    ImmutableList<DomContent> content =
        TextFormatter.formatText(
            "Enter your full legal name.",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true,
            /* ariaLabelForNewTabs= */ "");

    assertThat(content.get(0).render())
        .isEqualTo(
            """
<p>Enter your full legal name.<span class="usa-hint--required" aria-hidden="true">\u00a0*</span></p>
""");
  }

  @Test
  public void insertsRequiredIndicatorBeforeLists() {
    ImmutableList<DomContent> contentWithUnorderedList =
        TextFormatter.formatText(
            """
            Here is some text.
            * list item one
            * list item two""",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true,
            /* ariaLabelForNewTabs= */ "");
    String htmlContentWithUnorderedList = contentWithUnorderedList.get(0).render();
    assertThat(htmlContentWithUnorderedList)
        .isEqualTo(
            """
<p>Here is some text.<span class="usa-hint--required" aria-hidden="true"> *</span></p>
<ul class="usa-list margin-r-4"><li>list item one</li><li>list item two</li></ul>
""");

    ImmutableList<DomContent> contentWithOrderedList =
        TextFormatter.formatText(
            """
            Here is some text.
            1. list item one
            2. list item two""",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true,
            /* ariaLabelForNewTabs= */ "");
    String htmlContentWithOrderedList = contentWithOrderedList.get(0).render();
    assertThat(htmlContentWithOrderedList)
        .isEqualTo(
            """
<p>Here is some text.<span class="usa-hint--required" aria-hidden="true"> *</span></p>
<ol class="list-decimal mx-8"><li>list item one</li><li>list item two</li></ol>
""");
  }

  @Test
  public void insertsRequiredIndicatorAfterLists() {
    ImmutableList<DomContent> contentWithUnorderedList =
        TextFormatter.formatText(
            """
            - list item one
            - list item two
            - list item three""",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true,
            /* ariaLabelForNewTabs= */ "");
    String htmlContentWithUnorderedList = contentWithUnorderedList.get(0).render();
    assertThat(htmlContentWithUnorderedList)
        .isEqualTo(
            """
            <ul class="usa-list margin-r-4"><li>list item one</li><li>list item two</li><li>list item\
             three<span class="usa-hint--required" aria-hidden="true">\
             *</span></li></ul>
            """);

    ImmutableList<DomContent> contentWithOrderedList =
        TextFormatter.formatText(
            """
            1. list item one
            2. list item two
            3. list item three""",
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ true,
            /* ariaLabelForNewTabs= */ "");
    String htmlContentWithOrderedList = contentWithOrderedList.get(0).render();
    assertThat(htmlContentWithOrderedList)
        .isEqualTo(
            """
<ol class="list-decimal mx-8">\
<li>list item one</li>\
<li>list item two</li>\
<li>list item three\
<span class="usa-hint--required" aria-hidden="true"> *</span>\
</li></ol>
""");
  }

  @Test
  public void listRendersCorrectly() {
    String withList =
        """
        This is my list:
        * cream cheese
        * eggs
        * sugar
        * vanilla""";
    ImmutableList<DomContent> content = TextFormatter.formatTextForAdmins(withList);
    String htmlContent = content.get(0).render();

    assertThat(htmlContent)
        .isEqualTo(
            """
            <p>This is my list:</p>
            <ul class="usa-list margin-r-4">\
            <li>cream cheese</li>\
            <li>eggs</li>\
            <li>sugar</li>\
            <li>vanilla</li>\
            </ul>
            """);
  }

  @Test
  public void orderedListRendersCorrectly() {
    String withList =
        """
        This is my list:
        1. cream cheese

        **hello**

        2. eggs
        3. sugar
        4. vanilla
        """;

    ImmutableList<DomContent> content = TextFormatter.formatTextForAdmins(withList);
    String htmlContent = content.get(0).render();

    assertThat(htmlContent)
        .isEqualTo(
            """
<p>This is my list:</p>
<ol class="list-decimal mx-8"><li>cream cheese</li></ol>
<p><strong>hello</strong></p>
<ol start="2" class="list-decimal mx-8"><li>eggs</li><li>sugar</li><li>vanilla</li></ol>
""");
  }

  @Test
  public void preservesLines() {
    String withBlankLine =
        """
        This is the first line of content.

        This is the second (or third) line of content.


        This is the third (or sixth) line of content.""";

    ImmutableList<DomContent> preservedBlanksContent =
        TextFormatter.formatText(
            withBlankLine,
            /* preserveEmptyLines= */ true,
            /* addRequiredIndicator= */ false,
            /* ariaLabelForNewTabs= */ "");
    assertThat(preservedBlanksContent.get(0).render())
        .isEqualTo(
            """
            <p>This is the first line of content.<br /> </p>
            <p>This is the second (or third) line of content.<br /> </p>
            <p> </p>
            <p>This is the third (or sixth) line of content.</p>
            """);

    ImmutableList<DomContent> nonPreservedBlanksContent =
        TextFormatter.formatTextForAdmins(withBlankLine);
    assertThat(nonPreservedBlanksContent.get(0).render())
        .isEqualTo(
            """
            <p>This is the first line of content.</p>
            <p>This is the second (or third) line of content.</p>
            <p>This is the third (or sixth) line of content.</p>
            """);
  }

  @Test
  public void appliesTextEmphasis() {
    String stringWithMarkdown =
        "# Hello!\nThis is a string with *italics* and **bold** and `inline code`";
    ImmutableList<DomContent> formattedText = TextFormatter.formatTextForAdmins(stringWithMarkdown);
    assertThat(formattedText.get(0).render())
        .isEqualTo(
            """
            <p>Hello!</p>
            <p>This is a string with <em>italics</em> and <strong>bold</strong> and\
             <code>inline code</code></p>
            """);
  }

  @Test
  public void removesScriptTags() {
    String stringWithScriptTag = "<script>alert('bad-time');</script>";
    ImmutableList<DomContent> formattedText =
        TextFormatter.formatTextForAdmins(stringWithScriptTag);
    assertThat(formattedText.get(0).render()).isEqualTo("\n");
  }

  @Test
  public void replacesAllMarkdownHeadingsWithParagraphs() {
    String markdown =
        """
        # Heading 1
        ## Heading 2
        ### Heading 3
        #### Heading 4
        ##### Heading 5
        ###### Heading 6
        """;

    ImmutableList<DomContent> formattedText = TextFormatter.formatTextForAdmins(markdown);

    assertThat(formattedText.get(0).render())
        .isEqualTo(
            """
            <p>Heading 1</p>
            <p>Heading 2</p>
            <p>Heading 3</p>
            <p>Heading 4</p>
            <p>Heading 5</p>
            <p>Heading 6</p>
            """);
  }

  @Test
  public void rejectedElementsAndAttributesAreLogged() {
    Logger logger = (Logger) LoggerFactory.getLogger(TextFormatter.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    String stringWithBadAttributesAndElements =
        "<script>console.log('uhoh')</script><div id=\"bad-id\"></div>";
    TextFormatter.formatTextForAdmins(stringWithBadAttributesAndElements);

    ImmutableList<ILoggingEvent> logsList = ImmutableList.copyOf(listAppender.list);
    assertThat(logsList.get(0).getMessage())
        .isEqualTo("HTML element: \"script\" was caught and discarded.");
    assertThat(logsList.get(1).getMessage())
        .isEqualTo("HTML attribute: \"id\" was caught and discarded.");
  }

  @Test
  public void formatTextWithAriaLabel_addsAriaLabel() {
    ImmutableList<DomContent> content =
        TextFormatter.formatText(
            "[link](https://www.example.com)", false, false, "test aria label");

    assertThat(content.get(0).render()).contains("aria-label=\"link, test aria label\"");
  }

  @Test
  public void formatTextToSanitizedHTMLWithAriaLabel_addsAriaLabel() {
    String content =
        TextFormatter.formatTextToSanitizedHTML(
            "[link](https://www.example.com)", false, false, "test aria label");

    assertThat(content).contains("aria-label=\"link, test aria label\"");
  }

  @Test
  public void formatTextToSanitizedHTMLWithAriaLabel_removesScriptTags() {
    String stringWithScriptTag = "<script>alert('bad-time');</script>";
    String formattedText =
        TextFormatter.formatTextToSanitizedHTML(
            stringWithScriptTag,
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false,
            "test aria label");
    assertThat(formattedText).isEqualTo("\n");
  }

  @Test
  public void formatTextToSanitizedHTMLWithAriaLabel_appliesMarkdownFormatting() {
    String stringWithMarkdown =
        """
        # Hello!
        This is a string with *italics* and **bold** and `inline code`""";
    String formattedText =
        TextFormatter.formatTextToSanitizedHTML(
            stringWithMarkdown,
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false,
            "aria ");
    assertThat(formattedText)
        .isEqualTo(
            """
            <p>Hello!</p>
            <p>This is a string with <em>italics</em> and <strong>bold</strong> and\
             <code>inline code</code></p>
            """);
  }

  @Test
  public void formatTextToSanitizedHTML_emptyStringReturnsEmptyString() {
    assertThat(TextFormatter.formatTextToSanitizedHTML("", false, false, "")).isEmpty();
    assertThat(TextFormatter.formatTextToSanitizedHTML("", true, false, "")).isEmpty();
    assertThat(TextFormatter.formatTextToSanitizedHTML("", false, true, "")).isEmpty();
    assertThat(TextFormatter.formatTextToSanitizedHTML("", true, true, "")).isEmpty();
  }
}
