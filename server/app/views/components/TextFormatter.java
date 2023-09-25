package views.components;

import static j2html.TagCreator.rawHtml;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import views.CiviFormMarkdown;

/** The TextFormatter class formats text using Markdown and some custom logic. */
public final class TextFormatter {

  private static final CiviFormMarkdown CIVIFORM_MARKDOWN = new CiviFormMarkdown();

  /** Passes provided text through Markdown formatter. */
  public static ImmutableList<DomContent> formatText(String text, boolean preserveEmptyLines) {
    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();

    String markdownText = CIVIFORM_MARKDOWN.render(text);
    markdownText = TextFormatter.addSvgIcons(markdownText);
    markdownText = TextFormatter.replaceH1Tags(markdownText);
    builder.add(rawHtml(markdownText));
    return builder.build();
  }

  public static String addSvgIcons(String markdownText) {
    String closingATag = "</a>";
    String svgIconString =
        Icons.svg(Icons.OPEN_IN_NEW)
            .withClasses("shrink-0", "h-5", "w-auto", "inline", "ml-1", "align-text-top")
            .toString();
    return markdownText.replaceAll(closingATag, svgIconString + closingATag);
  }

  public static String replaceH1Tags(String markdownText) {
    String replaceOpenTags = markdownText.replaceAll("<h1>", "<h2>");
    return replaceOpenTags.replaceAll("</h1>", "</h2>");
  }
}
