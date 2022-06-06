package views.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import views.style.BaseStyles;
import views.style.Styles;

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
public class TextFormatter {
  public enum UrlOpenAction {
    SameTab,
    NewTab
  }

  private static final Logger logger = LoggerFactory.getLogger(TextFormatter.class);

  private static final String ACCORDION_CONTENT = ">";
  private static final String ACCORDION_HEADER = "### ";
  private static final String BULLETED_ITEM = "* ";

  public static ImmutableList<DomContent> createLinksAndEscapeText(
      String content, UrlOpenAction urlOpenAction) {
    // JAVASCRIPT option avoids including surrounding quotes or brackets in the URL.
    List<Url> urls = new UrlDetector(content, UrlDetectorOptions.JAVASCRIPT).detect();

    ImmutableList.Builder<DomContent> contentBuilder = ImmutableList.builder();
    for (Url url : urls) {
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

      int index = content.indexOf(url.getOriginalUrl());
      // Find where this URL is in the text.
      if (index == -1) {
        logger.error(
            String.format(
                "Detected URL %s not present in actual content, %s.",
                url.getOriginalUrl(), content));
        continue;
      }
      if (index > 0) {
        // If it's not at the beginning, add the text from before the URL.
        contentBuilder.add(text(content.substring(0, index)));
      }
      // Add the URL.
      var urlTag =
          a().withText(url.getOriginalUrl())
              .withHref(url.getFullUrl())
              .withClasses(BaseStyles.TEXT_SEATTLE_BLUE);

      if (urlOpenAction == UrlOpenAction.NewTab) {
        urlTag.withTarget("_blank")
                .with(Icons.svg(Icons.OPEN_IN_NEW_PATH, 24, 24)
                        .withClasses(Styles.FLEX_SHRINK_0, Styles.H_5, Styles.W_AUTO, Styles.INLINE, Styles.ML_1, Styles.ALIGN_TEXT_TOP));
      }
      contentBuilder.add(urlTag);

      content = content.substring(index + url.getOriginalUrl().length());
    }
    // If there's content leftover, add it.
    if (!Strings.isNullOrEmpty(content)) {
      contentBuilder.add(text(content));
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
        ImmutableList<DomContent> lineContent = TextFormatter.createLinksAndEscapeText(line, UrlOpenAction.NewTab);
        builder.add(div().with(lineContent));
      } else if (preserveEmptyLines) {
        builder.add(div().withClasses(Styles.H_6));
      }
    }
    return builder.build();
  }

  private static ContainerTag buildAccordion(String title, String accordionContent) {
    Accordion accordion = new Accordion().setTitle(title);
    ImmutableList<DomContent> contentTags = TextFormatter.formatText(accordionContent, true);
    contentTags.stream().forEach(tag -> accordion.addContent(tag));
    return accordion.getContainer();
  }

  private static ContainerTag buildList(ArrayList<String> items) {
    ContainerTag listTag = ul().withClasses(Styles.LIST_DISC, Styles.MX_8);
    items.forEach(item -> listTag.with(li().withText(item)));
    return listTag;
  }
}
