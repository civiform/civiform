package views;

import java.util.Map;
import org.commonmark.node.Document;
import org.commonmark.node.Link;
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
   * Removes p tags that are added by default by the CommonMark library. Logic was pulled from this
   * commit which is the recommended way to solve this problem:
   * https://github.com/commonmark/commonmark-java/commit/8b0fd7c73afaf1756edb6412c15a75cc423a6ba9
   */
  public String renderWithoutWrappingPTags(String markdown) {
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

  private static final Parser PARSER = Parser.builder().build();

  private static final HtmlRenderer RENDERER =
      HtmlRenderer.builder()
          .attributeProviderFactory(context -> new CiviFormAttributeProvider())
          .build();

  /** Customizes HTML element attributes for the CiviForm UI. */
  private static class CiviFormAttributeProvider implements AttributeProvider {

    @Override
    public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
      if (node instanceof Link) {
        attributes.put("class", BaseStyles.LINK_TEXT);
        attributes.put("target", "_blank");
      }
    }
  }
}
