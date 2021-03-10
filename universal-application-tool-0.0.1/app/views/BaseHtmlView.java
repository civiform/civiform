package views;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.mvc.Http;
import services.applicant.ValidationErrorMessage;
import views.html.helper.CSRF;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {

  public Tag renderHeader(String headerText) {
    return h1(headerText).withClasses(Styles.M_2);
  }

  public Tag renderLink(String text, String link) {
    return renderLink(text, link, "");
  }

  public Tag renderLink(String text, String link, String withClasses) {
    return a().withText(text)
        .withHref(link)
        .withClasses(BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT, withClasses);
  }

  public Tag renderLinkButton(String text, String link) {
    return renderLinkButton(text, link, "");
  }

  public Tag renderLinkButton(String text, String link, String withClasses) {
    return a(text)
        .withHref(link)
        .withClasses(
            Styles.FLOAT_LEFT,
            Styles.INLINE_BLOCK,
            Styles.CURSOR_POINTER,
            Styles.P_2,
            Styles.M_2,
            Styles.ROUNDED_MD,
            Styles.RING_BLUE_200,
            Styles.RING_OFFSET_2,
            Styles.BG_BLUE_400,
            Styles.TEXT_WHITE,
            StyleUtils.hover(Styles.BG_BLUE_500),
            StyleUtils.focus(ImmutableList.of(Styles.OUTLINE_NONE, Styles.RING_2)),
            withClasses);
  }

  protected ContainerTag fieldErrors(ImmutableSet<ValidationErrorMessage> errors) {
    return div(each(errors, error -> span(error.message())));
  }

  protected Tag checkboxInputWithLabel(
      String labelText, String inputId, String inputName, String inputValue) {
    return label()
        .with(
            input().withType("checkbox").withName(inputName).withValue(inputValue).withId(inputId),
            text(labelText));
  }

  protected Tag button(String textContents) {
    return TagCreator.button(text(textContents)).withType("button");
  }

  protected Tag button(String id, String textContents) {
    return button(textContents).withId(id);
  }

  protected Tag submitButton(String textContents) {
    return TagCreator.button(text(textContents)).withType("submit");
  }

  protected Tag submitButton(String id, String textContents) {
    return submitButton(textContents).withId(id);
  }

  protected Tag redirectButton(String id, String text, String redirectUrl) {
    return button(id, text).attr("onclick", String.format("window.location = '%s';", redirectUrl));
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
