package views;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.node.Document;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import views.style.BaseStyles;

/** Renders markdown to HTML with styles consistent with CiviForm's UI. */
public final class CiviFormMarkdown {

  /** Renders markdown to HTML with styles consistent with CiviForm's UI. */
  public String render(String markdown) {
    Node markdownRootNode = PARSER.parse(markdown);
    return RENDERER.render(markdownRootNode);
  }

  /**
   * This method removes the tags that wrap an HTML block. This is useful when you want to remove
   * the
   *
   * <p>tags that are added by default by the CommonMark library to strings that do not start with a
   * Markdown indicator. These inserted
   *
   * <p>tags can create paragraph breaks in places that are not intended. For example, in
   * TextFormatter.createLinksAndEscapeText, we apply Markdown parsing to the strings in between
   * urls. Allowing the default behavior of inserting
   *
   * <p>tags results in html that looks like this:
   *
   * <p>Markdown text: "The url www.kittens.com is the cutest url!"
   *
   * <p>Resulting rendered html:
   *
   * <p>The url <a>www.kittens.com</a>
   *
   * <p>is the cutest url! Logic for this method was pulled from this commit which is the
   * recommended way to solve this problem:
   * https://github.com/commonmark/commonmark-java/commit/8b0fd7c73afaf1756edb6412c15a75cc423a6ba9
   */
  public String renderWithoutWrappingTags(String markdown) {
    Document document = new Document();
    Node paragraphs = PARSER.parse(markdown);
    Node paragraph = paragraphs.getFirstChild();

    // paragraph is null when the string passed in is an empty space,
    // for example the space between two URLs passed through TextFormatter.createLinksAndEscapeText
    if (paragraph != null) {
      Node child = paragraph.getFirstChild();
      while (child != null) {
        Node current = child;
        child = current.getNext();
        document.appendChild(current);
      }
    }

    // PARSER.parse removes trailing spaces by default which is a problem when we have strings that
    // are the middle of a phrase, or have a string that is an empty space (as in the case when it's
    // the space between URLs). This adds back in spaces that may have been removed.
    if (markdown.substring(markdown.length() - 1).equals(" ")) {
      document.appendChild(new Text(" "));
    }

    return RENDERER.render(document);
  }

  private static final List<Extension> extensions = Arrays.asList(AutolinkExtension.create());

  private static final Parser PARSER = Parser.builder().extensions(extensions).build();

  private static final HtmlRenderer RENDERER =
      HtmlRenderer.builder()
          .extensions(extensions)
          .attributeProviderFactory(context -> new CiviFormAttributeProvider())
          .build();

  /** Customizes HTML element attributes for the CiviForm UI. */
  private static class CiviFormAttributeProvider implements AttributeProvider {

    @Override
    public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
      if (node instanceof Link) {
        attributes.put(
            "class", BaseStyles.LINK_TEXT + " " + BaseStyles.LINK_HOVER_TEXT + " underline");
        attributes.put("target", "_blank");
      } else if (node instanceof ListBlock) {
        attributes.put("class", "list-disc mx-8");
      }
    }
  }
}
