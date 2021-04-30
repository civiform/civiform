package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import play.mvc.Http;
import views.html.helper.CSRF;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {

  public static Tag renderHeader(String headerText, String... additionalClasses) {
    return h1(headerText).withClasses(Styles.M_2, StyleUtils.joinStyles(additionalClasses));
  }

  protected static ContainerTag fieldErrors(ImmutableSet<String> errors) {
    return div(each(errors, TagCreator::span));
  }

  protected static Tag checkboxInputWithLabel(
      String labelText, String inputId, String inputName, String inputValue) {
    return label()
        .with(
            input().withType("checkbox").withName(inputName).withValue(inputValue).withId(inputId),
            text(labelText));
  }

  protected static Tag button(String textContents) {
    return TagCreator.button(text(textContents)).withType("button");
  }

  protected static Tag button(String id, String textContents) {
    return button(textContents).withId(id);
  }

  protected static Tag submitButton(String textContents) {
    return TagCreator.button(text(textContents)).withType("submit");
  }

  protected static Tag submitButton(String id, String textContents) {
    return submitButton(textContents).withId(id);
  }

  protected static Tag redirectButton(String id, String text, String redirectUrl) {
    return button(id, text).attr("onclick", String.format("window.location = '%s';", redirectUrl));
  }

  /**
   * Generates a hidden HTML input tag containing a signed CSRF token. The token and tag must be
   * present in all UAT forms.
   */
  protected static Tag makeCsrfTokenInputTag(Http.Request request) {
    return input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken");
  }

  private static String getCsrfToken(Http.Request request) {
    return CSRF.getToken(request.asScala()).value();
  }

  protected String renderDateTime(Instant time) {
    LocalDateTime datetime = LocalDateTime.ofInstant(time, ZoneId.of("America/Los_Angeles"));
    return datetime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd 'at' h:mm a"));
  }
}
