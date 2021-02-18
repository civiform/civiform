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

import javax.inject.Inject;

/** Base class for all HTML views. Provides stateless convenience methods for generating HTML. */
abstract class BaseHtmlView {

  protected Tag textField(String fieldName, String labelText) {
    return label(text(labelText), input().withType("text").withName(fieldName))
        .attr("for", fieldName);
  }

  protected Tag passwordField(String fieldName, String labelText) {
    return label(text(labelText), input().withType("password").withName(fieldName))
        .attr("for", fieldName);
  }

  protected Tag submitButton(String textContents) {
    return input().withType("submit").withValue(textContents);
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
