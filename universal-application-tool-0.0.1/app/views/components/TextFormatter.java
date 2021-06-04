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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import views.style.Styles;

/**
 * The TextFormatter class introduces options for converting plain-text strings into richer HTML
 * content.
 *
 * <p>The following is currently supported:
 *
 * <ul>
 *   <li>Accordions
 *   <li>Bulleted lists
 *   <li.URL links
 * </ul>
 */
public class TextFormatter {
  private static final Logger LOG = LoggerFactory.getLogger(TextFormatter.class);

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

  public static ImmutableList<DomContent> createLinksAndEscapeText(String content) {
    List<Url> urls = new UrlDetector(content, UrlDetectorOptions.Default).detect();
    ImmutableList.Builder<DomContent> contentBuilder = ImmutableList.builder();
    for (Url url : urls) {
      int index = content.indexOf(url.getOriginalUrl());
      // Find where this URL is in the text.
      if (index == -1) {
        LOG.error(
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
      contentBuilder.add(
          a().withText(url.getOriginalUrl())
              .withHref(url.getFullUrl())
              .withClasses(Styles.OPACITY_75));
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
      if (line.startsWith("###")) { // We're calling this an accordion.
        String accordionHeader = line.substring(3);
        int next = i + 1;
        ArrayList<String> content = new ArrayList<>();
        while (next < lines.length && lines[next].startsWith(">")) {
          content.add(lines[next].substring(1));
          next++;
        }
        i = next - 1;
        String accordionContent = content.stream().collect(Collectors.joining("\n"));
        builder.add(buildAccordion(accordionHeader, accordionContent));
      } else if (line.startsWith("*")) { // unordered list item.
        ArrayList<String> items = new ArrayList<>();
        items.add(line.substring(1));
        int next = i + 1;
        while (next < lines.length && lines[next].startsWith("*")) {
          items.add(lines[next].substring(1));
          next++;
        }
        i = next - 1;
        builder.add(buildList(items));
      } else if (line.length() > 0) {
        ImmutableList<DomContent> lineContent = TextFormatter.createLinksAndEscapeText(line);
        builder.add(div().with(lineContent));
      } else if (preserveEmptyLines) {
        builder.add(div().withClasses(Styles.H_6));
      }
    }
    return builder.build();
  }
}
