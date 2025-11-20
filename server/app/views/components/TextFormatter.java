package views.components;

import static j2html.TagCreator.rawHtml;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import j2html.tags.DomContent;
import java.util.List;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import views.CiviFormMarkdown;
import views.ViewUtils;

/** The TextFormatter class formats text using Markdown and some custom logic. */
public final class TextFormatter {

  private static final Logger logger = LoggerFactory.getLogger(TextFormatter.class);
  private static final CiviFormMarkdown CIVIFORM_MARKDOWN = new CiviFormMarkdown();

  /**
   * Passes provided text through Markdown formatter. This is used by j2html to render strings
   * containing markdown
   */
  public static ImmutableList<DomContent> formatText(
      String text,
      boolean preserveEmptyLines,
      boolean addRequiredIndicator,
      String ariaLabelForNewTabs) {
    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();
    builder.add(
        rawHtml(
            formatTextToSanitizedHTML(
                text, preserveEmptyLines, addRequiredIndicator, ariaLabelForNewTabs)));
    return builder.build();
  }

  /**
   * Passes provided text through Markdown formatter with preserveEmptyLines and
   * addRequiredIndicator set to false. This function does not add translated aria labels to links
   * and should only be used in admin facing views.
   */
  public static ImmutableList<DomContent> formatTextForAdmins(String text) {
    return formatText(
        text,
        /* preserveEmptyLines= */ false,
        /* addRequiredIndicator= */ false,
        /* ariaLabelForNewTabs= */ "opens in a new tab");
  }

  /**
   * Passes provided text through Markdown formatter, returning a String with the sanitized HTML.
   * This is used by Thymeleaf to render strings containing markdown.
   */
  public static String formatTextToSanitizedHTML(
      String text,
      boolean preserveEmptyLines,
      boolean addRequiredIndicator,
      String ariaLabelForNewTabs) {
    if (text.isBlank()) {
      return "";
    }

    if (preserveEmptyLines) {
      text = preserveEmptyLines(text);
    }

    String markdownText = CIVIFORM_MARKDOWN.render(text);
    markdownText = addAriaLabelToLinks(markdownText, ariaLabelForNewTabs);
    if (addRequiredIndicator) {
      markdownText = addRequiredIndicator(markdownText);
    }

    return sanitizeHtml(markdownText);
  }

  private static String preserveEmptyLines(String text) {
    String[] lines = Iterables.toArray(Splitter.on("\n").split(text), String.class);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.isBlank()) {
        // Add an empty space character so the blank line is preserved
        lines[i] = "&nbsp;\n";
      }
    }
    return String.join("\n", lines);
  }

  private static String addAriaLabelToLinks(String markdownText, String ariaLabelForNewTabs) {
    // Use regex to find all <a> tags and add aria-label with link text + ariaLabelForNewTabs
    return markdownText.replaceAll(
        "<a ([^>]*)>([^<]+)</a>", "<a $1 aria-label=\"$2, " + ariaLabelForNewTabs + "\">$2</a>");
  }

  private static String addRequiredIndicator(String markdownText) {
    // If the question ends with a list (UL or OL tag), we need to handle the
    // required indicator differently
    if (endsWithListTag(markdownText, "</ul>\n")) {
      return handleRequiredQuestionsThatEndInAList(markdownText, "<ul");
    } else if (endsWithListTag(markdownText, "</ol>\n")) {
      return handleRequiredQuestionsThatEndInAList(markdownText, "<ol");
    }

    // If the question doesn't end with a list, add the required indicator on to the
    // end
    int indexOfClosingTag = markdownText.lastIndexOf("</");
    return buildStringWithRequiredIndicator(markdownText, indexOfClosingTag);
  }

  private static boolean endsWithListTag(String markdownText, String closingListTag) {
    int indexOfClosingListTag = markdownText.lastIndexOf(closingListTag);
    return indexOfClosingListTag > -1
        && markdownText.substring(indexOfClosingListTag).equals(closingListTag);
  }

  private static String handleRequiredQuestionsThatEndInAList(
      String markdownText, String openingListTag) {
    int indexOfOpeningListTag = markdownText.indexOf(openingListTag);
    // If the question has no text before the list, add the required indicator to
    // the end of the list before the closing LI tag. Otherwise, add the required
    // indicator to the paragraph that precedes the list
    return indexOfOpeningListTag == 0
        ? addRequiredIndicatorAfterList(markdownText)
        : addRequiredIndicatorBeforeList(markdownText, indexOfOpeningListTag);
  }

  private static String addRequiredIndicatorAfterList(String markdownText) {
    int indexOfClosingLiTag = markdownText.lastIndexOf("</li");
    return buildStringWithRequiredIndicator(markdownText, indexOfClosingLiTag);
  }

  private static String addRequiredIndicatorBeforeList(
      String markdownText, int indexOfOpeningListTag) {
    String substringWithoutList = markdownText.substring(0, indexOfOpeningListTag);
    int indexOfClosingTag = substringWithoutList.lastIndexOf("</");
    return buildStringWithRequiredIndicator(markdownText, indexOfClosingTag);
  }

  private static String buildStringWithRequiredIndicator(
      String markdownText, int indexOfClosingTag) {
    String insertTextAfterRequiredIndicator = markdownText.substring(indexOfClosingTag);
    String markdownWithRequiredIndicator =
        ViewUtils.requiredQuestionIndicator().toString().concat(insertTextAfterRequiredIndicator);
    return markdownText.substring(0, indexOfClosingTag) + markdownWithRequiredIndicator;
  }

  public static String sanitizeHtml(String markdownText) {
    PolicyFactory customPolicy =
        new HtmlPolicyBuilder()
            .allowElements(
                "p", "div", "a", "ul", "ol", "li", "hr", "span", "svg", "br", "em", "strong",
                "code", "path", "pre")
            .allowElements(
                // Convert all heading tags (h1-h6) to paragraph tags to prevent multiple h1s
                // on a page, which violates accessibility best practices
                (String elementName, List<String> attrs) -> "p", "h1", "h2", "h3", "h4", "h5", "h6")
            .allowWithoutAttributes()
            .allowAttributes(
                "class",
                "target",
                "xmlns",
                "fill",
                "start", // <--- Allow OLs to continue numbering instead of resetting to 1.
                "stroke",
                "stroke-width",
                "aria-label",
                "aria-hidden",
                "viewBox", // <--- This is for SVGs and it **IS** case-sensitive
                "d",
                "role")
            .globally()
            .toFactory();

    PolicyFactory policy = customPolicy.and(Sanitizers.LINKS);
    return policy.sanitize(markdownText, buildHtmlChangeListener(), /* context= */ null);
  }

  private static HtmlChangeListener<Object> buildHtmlChangeListener() {
    return new HtmlChangeListener<Object>() {
      @Override
      public void discardedTag(Object ctx, String elementName) {
        logger.warn(String.format("HTML element: \"%s\" was caught and discarded.", elementName));
      }

      @Override
      public void discardedAttributes(Object ctx, String tagName, String... attributeNames) {
        for (String attribute : attributeNames) {
          logger.warn(String.format("HTML attribute: \"%s\" was caught and discarded.", attribute));
        }
      }
    };
  }
}
