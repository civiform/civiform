package views.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.UlTag;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import views.style.BaseStyles;

/**
 * The TextFormatter class introduces options for converting plain-text strings into richer HTML
 * content.
 *
 * <p>The following is currently supported:
 *
 * <ul>
 *   <li>Accordions (Headers start with "### " and following lines starting with ">" indicate
 *       content)
 *   <li>Bulleted lists (Lines starting with "* "")
 *   <li>URL links
 * </ul>
 */
public final class TextFormatter {
  public enum UrlOpenAction {
    SameTab,
    NewTab
  }

  private static final Logger logger = LoggerFactory.getLogger(TextFormatter.class);

  private static final String ACCORDION_CONTENT = ">";
  private static final String ACCORDION_HEADER = "### ";
  private static final String BULLETED_ITEM = "* ";

  /**
   * Parses plain-text string into rich HTML with clickable links.
   *
   * @param content The plain-text string to parse.
   * @param urlOpenAction Enum indicating whether the url should open in a new tab.
   * @param addRequiredIndicator Whether a required indicator, in the form of a red asterisk, should
   *     be added to the html element.
   */
  public static ImmutableList<DomContent> createLinksAndEscapeText(
      String content, UrlOpenAction urlOpenAction, boolean addRequiredIndicator) {
    // JAVASCRIPT option avoids including surrounding quotes or brackets in the URL.
    List<Url> urls = new UrlDetector(content, UrlDetectorOptions.JAVASCRIPT).detect();

    ImmutableList.Builder<DomContent> contentBuilder = ImmutableList.builder();
    for (int i = 0; i < urls.size(); i++) {
      Url url = urls.get(i);
      try {
        // While technically they could be part of the URL, trailing punctuation
        // is more likely to be part of the surrounding text, so we strip.
        url = Url.create(StringUtils.stripEnd(url.getOriginalUrl(), ".?!,:;"));
      } catch (MalformedURLException e) {
        logger.error(
            String.format(
                "Failed to parse URL %s after stripping trailing punctuation",
                url.getOriginalUrl()));
      }

      int urlStartIndex = content.indexOf(url.getOriginalUrl());
      // Find where this URL is in the text.
      if (urlStartIndex == -1) {
        logger.error(
            String.format(
                "Detected URL %s not present in actual content, %s.",
                url.getOriginalUrl(), content));
        continue;
      }

      // If this URL looks like it's actually an email address, skip it.
      if (EmailValidator.getInstance().isValid(url.getOriginalUrl())) {
        continue;
      }
      // If immediately following this URL there's an '@' and another URL, they could
      // be two parts of an email address, so skip them both in case.
      // [0] [1] [2]
      // len: 3
      if (i < (urls.size() - 1)) {
        int nextUrlStartIndex = content.indexOf(urls.get(i + 1).getOriginalUrl());
        if (content.charAt(nextUrlStartIndex - 1) == '@') {
          i = i + 1;
          continue;
        }
      }

      if (urlStartIndex > 0) {
        // If it's not at the beginning, add the text from before the URL.
        contentBuilder.add(text(content.substring(0, urlStartIndex)));
      }
      // Add the URL.
      var urlTag =
          a().withText(url.getOriginalUrl())
              .withHref(url.getFullUrl())
              .withClasses(BaseStyles.TEXT_SEATTLE_BLUE);

      if (urlOpenAction == UrlOpenAction.NewTab) {
        urlTag
            .withTarget("_blank")
            .with(
                Icons.svg(Icons.OPEN_IN_NEW)
                    .withClasses("shrink-0", "h-5", "w-auto", "inline", "ml-1", "align-text-top"));
      }
      contentBuilder.add(urlTag);

      content = content.substring(urlStartIndex + url.getOriginalUrl().length());
    }
    // If there's content leftover, add it.
    if (!Strings.isNullOrEmpty(content)) {
      contentBuilder.add(text(content));
    }
    if (addRequiredIndicator) {
      contentBuilder.add(span(rawHtml("&nbsp;*")).withClasses("text-red-600"));
    }
    return contentBuilder.build();
  }

  /** Adds the ability to create accordions and lists from data in text fields. */
  public static ImmutableList<DomContent> formatText(String text, boolean preserveEmptyLines) {
    String[] lines = Iterables.toArray(Splitter.on("\n").split(text), String.class);
    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.startsWith(TextFormatter.ACCORDION_HEADER)) { // We're calling this an accordion.
        String accordionHeader = line.substring(3);
        int next = i + 1;
        ArrayList<String> content = new ArrayList<>();
        while (next < lines.length && lines[next].startsWith(TextFormatter.ACCORDION_CONTENT)) {
          content.add(lines[next].substring(1));
          next++;
        }
        i = next - 1;
        String accordionContent = content.stream().collect(Collectors.joining("\n"));
        builder.add(buildAccordion(accordionHeader, accordionContent));
      } else if (line.startsWith(TextFormatter.BULLETED_ITEM)) { // unordered list item.
        ArrayList<String> items = new ArrayList<>();
        items.add(line.substring(1).trim());
        int next = i + 1;
        while (next < lines.length && lines[next].startsWith(TextFormatter.BULLETED_ITEM)) {
          items.add(lines[next].substring(1).trim());
          next++;
        }
        i = next - 1;
        builder.add(buildList(items));
      } else if (line.length() > 0) {
        ImmutableList<DomContent> lineContent =
            TextFormatter.createLinksAndEscapeText(
                line, UrlOpenAction.NewTab, /*addRequiredIndicator= */ false);
        builder.add(div().with(lineContent));
      } else if (preserveEmptyLines) {
        builder.add(div().withClasses("h-6"));
      }
    }
    return builder.build();
  }

  private static DivTag buildAccordion(String title, String accordionContent) {
    Accordion accordion = new Accordion().setTitle(title);
    ImmutableList<DomContent> contentTags = TextFormatter.formatText(accordionContent, true);
    contentTags.stream().forEach(accordion::addContent);
    return accordion.getContainer();
  }

  private static UlTag buildList(ArrayList<String> items) {
    UlTag listTag = ul().withClasses("list-disc", "mx-8");
    items.forEach(item -> listTag.with(li().withText(item)));
    return listTag;
  }
}
