package views.components;

import static com.google.common.base.Preconditions.checkNotNull;


import static j2html.TagCreator.rawHtml;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import j2html.tags.DomContent;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import views.CiviFormMarkdown;
import views.ViewUtils;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import services.MessageKey;
import javax.inject.Inject;


import org.apache.commons.lang3.StringUtils;


/** The TextFormatter class formats text using Markdown and some custom logic. */
public final class TextFormatter {

  private static final Logger logger = LoggerFactory.getLogger(TextFormatter.class);
  private static final CiviFormMarkdown CIVIFORM_MARKDOWN = new CiviFormMarkdown();

  public static ImmutableList<DomContent> formatText(String text, boolean preserveEmptyLines, boolean addRequiredIndicator, Messages messages) {
    // do the messages thing here
    // call the other method


    CIVIFORM_MARKDOWN.setAriaLabel(messages.at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName()).toLowerCase(Locale.ROOT));

    return formatText(text, preserveEmptyLines, addRequiredIndicator);
  }

  /** Passes provided text through Markdown formatter. */
  public static ImmutableList<DomContent> formatText(
      String text, boolean preserveEmptyLines, boolean addRequiredIndicator) {

    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();

    if (preserveEmptyLines) {
      text = preserveEmptyLines(text);
    }
    
    String markdownText = CIVIFORM_MARKDOWN.render(text); // pass in request here
    markdownText = addIconToLinks(markdownText);
    // markdownText = addAriaLabelsToLinks(markdownText, messages);
    markdownText = addTextSize(markdownText);
    if (addRequiredIndicator) {
      markdownText = addRequiredIndicator(markdownText);
    }

    builder.add(rawHtml(sanitizeHtml(markdownText)));
    return builder.build();
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

  private static String addIconToLinks(String markdownText) {
    String closingATag = "</a>";
    String svgIconString =
        Icons.svg(Icons.OPEN_IN_NEW)
            .withClasses("shrink-0", "h-5", "w-auto", "inline", "ml-1", "align-text-top")
            .toString();
    return markdownText.replaceAll(closingATag, svgIconString + closingATag);
  }

  private static String addAriaLabelsToLinks(String markdownText, Messages messages) {
    String preTexString = "rel=\"nofollow noopener noreferrer\">";
    String postTextString = "<svg";
    String[] textStringsArr = StringUtils.substringsBetween(markdownText, preTexString, postTextString);
    // how can we get the text to use for the aria label? poop.
    List<String> textStringsList = Arrays.asList(textStringsArr);
    textStringsList.forEach((textString) -> {
      // how do we add the aria label into the correct place arghhhhhhhh
          String ariaLabel = "aria-label=\"";

    });

    return markdownText;
  }

  private static String addTextSize(String markdownText) {
    // h1 and h2 tags are set to "text-2xl" and "text-xl" respectively in styles.css
    String replacedH3Tags = markdownText.replaceAll("<h3>", "<h3 class=\"text-lg\">");
    return replacedH3Tags.replaceAll("<h4>", "<h4 class=\"text-base\">");
  }

  private static String addRequiredIndicator(String markdownText) {
    int indexOfClosingTag = markdownText.lastIndexOf("</");
    String closingTag = markdownText.substring(indexOfClosingTag);
    String stringWithRequiredIndicator =
        ViewUtils.requiredQuestionIndicator().toString().concat(closingTag);
    return markdownText.substring(0, indexOfClosingTag) + stringWithRequiredIndicator;
  }

  private static String sanitizeHtml(String markdownText) {
    PolicyFactory customPolicy =
        new HtmlPolicyBuilder()
            .allowElements(
                "p", "div", "h2", "h3", "h4", "h5", "h6", "a", "ul", "ol", "li", "hr", "span",
                "svg", "br", "em", "strong", "code", "path", "pre")
            // Per accessibility best practices, we want to disallow adding h1 headers to
            // ensure the page does not have more than one h1 header
            // https://www.a11yproject.com/posts/how-to-accessible-heading-structure/
            // This logic changes h1 headers to h2 headers which are still larger than the default
            // text
            .allowElements(
                (String elementName, List<String> attrs) -> {
                  return "h2";
                },
                "h1")
            .allowWithoutAttributes()
            .allowAttributes(
                "class",
                "target",
                "xmlns",
                "fill",
                "stroke",
                "stroke-width",
                "aria-label",
                "aria-hidden",
                "viewbox",
                "d")
            .globally()
            .toFactory();

    PolicyFactory policy = customPolicy.and(Sanitizers.LINKS);
    return policy.sanitize(markdownText, buildHtmlChangeListener(), /*context=*/ null);
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
