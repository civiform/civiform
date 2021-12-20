package views;

import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Http;
import services.applicant.ValidationErrorMessage;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.html.helper.CSRF;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;
import java.util.stream.Stream;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {

  public static Tag renderHeader(String headerText, String... additionalClasses) {
    return h1(headerText).withClasses(Styles.MB_4, StyleUtils.joinStyles(additionalClasses));
  }

  public static ContainerTag fieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
    return div(each(errors, error -> div(error.getMessage(messages))))
        .withClasses(BaseStyles.FORM_ERROR_TEXT_BASE);
  }

  public static Tag button(String textContents) {
    return TagCreator.button(text(textContents)).withType("button");
  }

  public static Tag button(String id, String textContents) {
    return button(textContents).withId(id);
  }

  protected static Tag submitButton(String textContents) {
    return TagCreator.button(text(textContents)).withType("submit");
  }

  protected static Tag submitButton(String id, String textContents) {
    return submitButton(textContents).withId(id);
  }

  public static Tag redirectButton(String id, String text, String redirectUrl) {
    return button(id, text)
        .attr("onclick", String.format("window.location = '%s';", redirectUrl))
        .withClasses(Styles.M_2);
  }

  protected static Tag space() {
    return span(" ");
  }

  protected static Tag period() {
    return span(".");
  }

  protected static Tag spanNowrap(String tag) {
    return span(tag).withClasses(Styles.WHITESPACE_NOWRAP);
  }

  protected static Tag spanNowrap(Tag ... tags) {
    return span().with(tags).withClasses(Styles.WHITESPACE_NOWRAP);
  }

  /**
   * Generates a hidden HTML input tag containing a signed CSRF token. The token and tag must be
   * present in all CiviForm forms.
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

  protected ContainerTag renderPaginationDiv(
      int page, int pageCount, Function<Integer, Call> linkForPage) {
    ContainerTag div = div();
    if (page <= 1) {
      div.with(new LinkElement().setText("∅").asButton());
    } else {
      div.with(
          new LinkElement().setText("←").setHref(linkForPage.apply(page - 1).url()).asButton());
    }
    div.with(
        div("Page " + page + " of " + pageCount)
            .withClasses(
                Styles.LEADING_3, Styles.FLOAT_LEFT, Styles.INLINE_BLOCK, Styles.P_2, Styles.M_4));
    if (pageCount > page) {
      div.with(
          new LinkElement().setText("→").setHref(linkForPage.apply(page + 1).url()).asButton());
    } else {
      div.with(new LinkElement().setText("∅").asButton());
    }
    return div.with(br());
  }

  protected ContainerTag renderSearchForm(
      Http.Request request, Optional<String> search, Call searchCall) {
    return form()
        .withMethod("GET")
        .withAction(searchCall.url())
        .with(
            FieldWithLabel.input()
                .setId("search-field")
                .setFieldName("search")
                .setLabelText("Search")
                .setValue(search.orElse(""))
                .setPlaceholderText("Search")
                .getContainer()
                .withClasses(Styles.W_1_4),
            makeCsrfTokenInputTag(request),
            submitButton("Search").withClasses(Styles.M_2));
  }
}
