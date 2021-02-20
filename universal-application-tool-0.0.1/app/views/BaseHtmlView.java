package views;

import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;

import j2html.TagCreator;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import play.mvc.Http;
import play.twirl.api.Content;
import views.html.helper.CSRF;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {

  protected Content htmlContent(DomContent... domContents) {
    return new BaseHtmlLayout.HtmlResponseContent(domContents);
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

  protected Tag button(String id, String text) {
    return button(text).withId(id);
  }

  protected Tag button(String text) {
    return TagCreator.button(text(text)).withType("button");
  }

  protected Tag redirectButton(String id, String text, String redirectUrl) {
    return button(id, text).attr("onclick", String.format("window.location = '%s';", redirectUrl));
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
  protected Tag makeCsrfTokenInputTag(Http.Request request) {
    return input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken");
  }

  private String getCsrfToken(Http.Request request) {
    return CSRF.getToken(request.asScala()).value();
  }
}
