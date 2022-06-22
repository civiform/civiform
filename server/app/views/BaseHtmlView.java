package views;

import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.InputTag;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Http;
import services.applicant.ValidationErrorMessage;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.html.helper.CSRF;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {

  public static H1Tag renderHeader(String headerText, String... additionalClasses) {
    return h1(headerText).withClasses(Styles.MB_4, StyleUtils.joinStyles(additionalClasses));
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
    return asRedirectButton(
        TagCreator.button(text).withId(id).withClasses(Styles.M_2), redirectUrl);
  }

  protected static ButtonTag asRedirectButton(ButtonTag buttonEl, String redirectUrl) {
    return buttonEl.attr("onclick", String.format("window.location = '%s';", redirectUrl));
  }

  protected static ButtonTag makeSvgTextButton(String buttonText, String svgPath) {
    return TagCreator.button()
        .with(
            Icons.svg(svgPath, 18).withClasses(Styles.ML_1, Styles.MR_2, Styles.INLINE_BLOCK),
            span(buttonText));
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
        div(paginationText)
            .withClasses(
                Styles.LEADING_3, Styles.FLOAT_LEFT, Styles.INLINE_BLOCK, Styles.P_2, Styles.M_4));
    if (pageCount > page) {
      div.with(
          new LinkElement().setText("→").setHref(linkForPage.apply(page + 1).url()).asButton());
    } else {
      div.with(new LinkElement().setText("∅").asButtonNoHref());
    }
    return div.with(br());
  }

  protected FormTag renderSearchForm(
      Http.Request request, Optional<String> search, Call searchCall) {
    return renderSearchForm(
        request,
        search,
        searchCall,
        /* htmlClasses= */ Optional.empty(),
        /* labelText= */ Optional.empty());
  }

  protected FormTag renderSearchForm(
      Http.Request request,
      Optional<String> search,
      Call searchCall,
      Optional<String> htmlClasses,
      Optional<String> labelText) {
    return form()
        .withMethod("GET")
        .withAction(searchCall.url())
        .with(
            FieldWithLabel.input()
                .setId("search-field")
                .setFieldName("search")
                .setLabelText(labelText.orElse("Search"))
                .setValue(search.orElse(""))
                .setPlaceholderText("Search")
                .getInputTag()
                .withClasses(htmlClasses.orElse(Styles.W_1_4)),
            makeCsrfTokenInputTag(request),
            submitButton("Search").withClasses(Styles.M_2));
  }

  protected static ButtonTag toLinkButtonForPost(
      ButtonTag buttonEl, String href, Http.Request request) {
    String formId = RandomStringUtils.randomAlphabetic(32);
    FormTag hiddenForm =
        form()
            .withId(formId)
            .withClass(Styles.HIDDEN)
            .withMethod("POST")
            .withAction(href)
            .with(input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken"));

    return buttonEl.withForm(formId).with(hiddenForm);
  }
}
