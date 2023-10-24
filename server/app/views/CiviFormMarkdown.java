package views;

import java.util.Map;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import views.style.ApplicantStyles;

/** Renders markdown to HTML with styles consistent with CiviForm's UI. */
public final class CiviFormMarkdown {

  /** Renders markdown to HTML with styles consistent with CiviForm's UI. */
  public String render(String markdown) {
    Node markdownRootNode = PARSER.parse(markdown);
    return RENDERER.render(markdownRootNode);
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
        attributes.put("class", ApplicantStyles.LINK);
        attributes.put("target", "_blank");
      }
    }
  }
}
