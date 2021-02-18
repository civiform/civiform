package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.document;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import play.mvc.Http;
import play.twirl.api.Content;
import views.html.helper.CSRF;

/** Base class for all HTML views. Provides stateless convenience methods for generating HTML. */
abstract class BaseHtmlView {

  protected Content htmlContent(DomContent... domContents) {
    return new HtmlResponseContent(domContents);
  }

  protected Tag textField(String fieldName, String labelText) {
    return label(text(labelText), input().withType("text").withName(fieldName))
        .attr("for", fieldName);
  }

  protected Tag textField(String id, String fieldName, String labelText) {
    return label(text(labelText), input().withType("text").withName(fieldName).withId(id))
        .attr("for", fieldName);
  }

  protected Tag passwordField(String id, String fieldName, String labelText) {
    return label(text(labelText), input().withType("password").withName(fieldName).withId(id))
        .attr("for", fieldName);
  }

  protected Tag passwordField(String fieldName, String labelText) {
    return label(text(labelText), input().withType("password").withName(fieldName))
        .attr("for", fieldName);
  }

  protected Tag submitButton(String textContents) {
    return input().withType("submit").withValue(textContents);
  }

  protected Tag submitButton(String id, String textContents) {
    return input().withType("submit").withId(id).withValue(textContents);
  }

  /**
   * Generates a hidden HTML input tag containing a signed CSRF token. The token and tag must be
   * present in all UAT forms.
   */
  Tag makeCsrfTokenInputTag(Http.Request request) {
    return input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken");
  }

  private String getCsrfToken(Http.Request request) {
    return CSRF.getToken(request.asScala()).value();
  }

  protected static class HtmlResponseContent implements Content {
    private final DomContent[] domContents;

    protected HtmlResponseContent(DomContent... domContents) {
      this.domContents = checkNotNull(domContents);
    }

    @Override
    public String body() {
      return document(new ContainerTag("html").with(domContents));
    }

    @Override
    public String contentType() {
      return "text/html; charset=UTF-8";
    }
  }
}
