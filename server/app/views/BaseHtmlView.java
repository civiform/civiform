package views;

import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.Tag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.SpanTag;
import java.util.function.Function;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Http;
import services.applicant.ValidationErrorMessage;
import views.components.Icons;
import views.components.LinkElement;
import views.html.helper.CSRF;
import views.style.BaseStyles;
import views.style.StyleUtils;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {

  public static H1Tag renderHeader(String headerText, String... additionalClasses) {
    return h1(headerText).withClasses("mb-4", StyleUtils.joinStyles(additionalClasses));
  }

  public static DivTag fieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors, String... additionalClasses) {
    return div(each(errors, error -> div(error.getMessage(messages))))
        .withClasses(BaseStyles.FORM_ERROR_TEXT_BASE, StyleUtils.joinStyles(additionalClasses));
  }

  public static ButtonTag button(String textContents) {
    return TagCreator.button(text(textContents)).withType("button");
  }

  public static ButtonTag button(String id, String textContents) {
    return button(textContents).withId(id);
  }

  protected static ButtonTag submitButton(String textContents) {
    return TagCreator.button(text(textContents)).withType("submit");
  }

  protected static ButtonTag submitButton(String id, String textContents) {
    return submitButton(textContents).withId(id);
  }

  protected static ButtonTag redirectButton(String id, String text, String redirectUrl) {
    return asRedirectElement(TagCreator.button(text).withId(id).withClasses("m-2"), redirectUrl);
  }

  /**
   * Turns provided element into a clickable element. Upon click the user will be redirected to the
   * provided url. It's up to caller of this method to style element appropriately to make it clear
   * that the element is clickable. For example add hover effect and change cursor style.
   *
   * @return The element itself.
   */
  protected static <T extends Tag> T asRedirectElement(T element, String redirectUrl) {
    // Attribute `data-redirect-to` is handled in JS by main.ts file.
    element.attr("data-redirect-to", redirectUrl);
    return element;
  }

  protected static ButtonTag makeSvgTextButton(String buttonText, Icons icon) {
    return ViewUtils.makeSvgTextButton(buttonText, icon);
  }

  protected static SpanTag spanNowrap(String tag) {
    return span(tag).withClasses("whitespace-nowrap");
  }

  protected static SpanTag spanNowrap(Tag... tags) {
    return span().with(tags).withClasses("whitespace-nowrap");
  }

  /**
   * Generates a hidden HTML input tag containing a signed CSRF token. The token and tag must be
   * present in all CiviForm forms.
   */
  protected static InputTag makeCsrfTokenInputTag(Http.Request request) {
    return input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken");
  }

  private static String getCsrfToken(Http.Request request) {
    return CSRF.getToken(request.asScala()).value();
  }

  protected DivTag renderPaginationDiv(
      int page, int pageCount, Function<Integer, Call> linkForPage) {
    DivTag div = div();
    if (page <= 1) {
      div.with(new LinkElement().setText("∅").asButtonNoHref());
    } else {
      div.with(
          new LinkElement().setText("←").setHref(linkForPage.apply(page - 1).url()).asButton());
    }
    String paginationText =
        pageCount > 0 ? String.format("Page %d of %d", page, pageCount) : "No results";
    div.with(
        div(paginationText).withClasses("leading-3", "float-left", "inline-block", "p-2", "m-4"));
    if (pageCount > page) {
      div.with(
          new LinkElement().setText("→").setHref(linkForPage.apply(page + 1).url()).asButton());
    } else {
      div.with(new LinkElement().setText("∅").asButtonNoHref());
    }
    return div.with(br());
  }

  protected static ButtonTag toLinkButtonForPost(
      ButtonTag buttonEl, String href, Http.Request request) {
    String formId = RandomStringUtils.randomAlphabetic(32);
    FormTag hiddenForm =
        form()
            .withId(formId)
            .withClass("hidden")
            .withMethod("POST")
            .withAction(href)
            .with(input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken"));

    return buttonEl.withForm(formId).with(hiddenForm);
  }

  protected static final PTag requiredFieldsExplanationContent() {
    return p().with(
            span("Note: Fields marked with a ").withClass("text-gray-600"),
            span("*").withClass("text-red-700"),
            span(" are required.").withClass("text-gray-600"))
        .withClass("text-sm");
  }
}
