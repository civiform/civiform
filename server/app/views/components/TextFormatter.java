package views.components;

import static j2html.TagCreator.rawHtml;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import views.CiviFormMarkdown;
import views.ViewUtils;

/** The TextFormatter class formats text using Markdown and some custom logic. */
public final class TextFormatter {

  private static final CiviFormMarkdown CIVIFORM_MARKDOWN = new CiviFormMarkdown();

  /** Passes provided text through Markdown formatter. */
  public static ImmutableList<DomContent> formatText(
      String text, boolean preserveEmptyLines, boolean addRequiredIndicator) {

    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();

    if (preserveEmptyLines) {
      text = text.replaceAll("\n", "<br/>");
    }

    String markdownText = CIVIFORM_MARKDOWN.render(text);
    markdownText = addIconToLinks(markdownText);
    markdownText = replaceH1Tags(markdownText);
    if (addRequiredIndicator) {
      markdownText = addRequiredIndicatorInsidePTag(markdownText);
    }

    builder.add(rawHtml(sanitizeHtml(markdownText)));
    return builder.build();
  }

  private static String addIconToLinks(String markdownText) {
    String closingATag = "</a>";
    String svgIconString =
        Icons.svg(Icons.OPEN_IN_NEW)
            .withClasses("shrink-0", "h-5", "w-auto", "inline", "ml-1", "align-text-top")
            .toString();
    return markdownText.replaceAll(closingATag, svgIconString + closingATag);
  }

  // maybe do this with sanitizer??
  private static String replaceH1Tags(String markdownText) {
    String replaceOpenTags = markdownText.replaceAll("<h1>", "<h2>");
    return replaceOpenTags.replaceAll("</h1>", "</h2>");
  }

  private static String addRequiredIndicatorInsidePTag(String markdownText) {
    int indexOfPTag = markdownText.lastIndexOf("</p>");
    String stringWithRequiredIndicator = ViewUtils.requiredQuestionIndicator().toString() + "</p>";
    return markdownText.substring(0, indexOfPTag) + stringWithRequiredIndicator;
  }

  private static String sanitizeHtml(String markdownText) {
    PolicyFactory customPolicy =
        new HtmlPolicyBuilder()
            .allowElements(
                "p", "div", "h2", "h3", "h4", "h5", "h6", "a", "ul", "li", "span", "svg", "br",
                "em", "strong", "code", "path", "pre")
            .allowWithoutAttributes()
            .allowAttributes("class", "target")
            .globally()
            .toFactory();

    // Maybe log bad actors?

    PolicyFactory policy = customPolicy.and(Sanitizers.LINKS);
    return policy.sanitize(markdownText);
  }
}
