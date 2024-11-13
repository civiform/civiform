package views;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.li;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.Tag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.H2Tag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.SpanTag;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Http;
import services.MessageKey;
import services.RandomStringUtils;
import services.applicant.ValidationErrorMessage;
import views.components.Icons;
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

  public static H2Tag renderSubHeader(String headerText, String... additionalClasses) {
    return h2(headerText).withClasses("mb-4", StyleUtils.joinStyles(additionalClasses));
  }

  public static DivTag fieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors, String... additionalClasses) {
    return div(each(errors, error -> div(error.getMessage(messages))))
        .withClasses(BaseStyles.FORM_ERROR_TEXT_BASE, StyleUtils.joinStyles(additionalClasses));
  }

  /**
   * Creates a button that doesn't display any text but includes an aria label, which is needed for
   * accessibility. The icon SVG should be displayed as a child of this button.
   *
   * @param ariaLabel a label that will be used by screenreaders and other accessibility services to
   *     describe the button's purpose.
   */
  public static ButtonTag iconOnlyButton(String ariaLabel) {
    return TagCreator.button().withType("button").attr("aria-label", ariaLabel);
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

  protected static ButtonTag redirectButton(String text, String redirectUrl) {
    return asRedirectElement(TagCreator.button(text).withClasses("m-2"), redirectUrl);
  }

  protected static ButtonTag redirectButton(String id, String text, String redirectUrl) {
    return redirectButton(text, redirectUrl).withId(id);
  }

  /**
   * Turns provided element into a clickable element. Upon click the user will be redirected to the
   * provided url. It's up to caller of this method to style element appropriately to make it clear
   * that the element is clickable. For example add hover effect and change cursor style.
   *
   * @return The element itself.
   */
  public static <T extends Tag> T asRedirectElement(T element, String redirectUrl) {
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

  /**
   * Creates a USWDS pagination component. <a
   * href="https://designsystem.digital.gov/components/pagination/">USWDS pagination</a>
   *
   * <ul>
   *   <li>The component features a maximum of seven slots.
   *   <li>Each slot can contain a navigation item or an overflow indicator (ellipses).
   *   <li>If there are fewer than seven pages in the set, show only that number of slots.
   *   <li>The pages to the left and right of the current page must be shown if available.
   * </ul>
   *
   * @param page The current page number
   * @param pageCount The total number of pages
   * @param linkForPage The href for the current view
   * @param optionalMessages Optional messages to be used for i18n
   * @return NavTag
   */
  protected static NavTag renderPagination(
      int page,
      int pageCount,
      Function<Integer, Call> linkForPage,
      Optional<Messages> optionalMessages) {
    List<Integer> pageRange =
        IntStream.range(2, pageCount + 1).boxed().collect(Collectors.toList());

    boolean isCurrentPageInMiddle = page > 4 && page < (pageCount - 3);
    boolean isCurrentPageInLastFour = page >= (pageCount - 3);

    return nav()
        .withClass("usa-pagination")
        .attr("aria-label", "Pagination")
        .with(
            ul().withClass("usa-pagination__list")
                .condWith(page != 1, renderPreviousPageButton(page, linkForPage, optionalMessages))
                .with(
                    // Always show first page
                    renderPaginationPageButton(1, page == 1, linkForPage))

                // If page count is <= 7, there will be no ellipses.  Just show each page.
                .condWith(
                    pageCount <= 7,
                    each(
                        pageRange,
                        pageNum ->
                            renderPaginationPageButton(pageNum, page == pageNum, linkForPage)))

                /* If the page count is > 7 and there is a sufficient gap between the edges,
                ellipses will be in slots 2 and 6, with current page and adjacent pages in the middle.
                For example: [1] [...] [4] [!5!] [6] [...] [8] (Current page is 5) */
                .condWith(
                    pageCount > 7 && isCurrentPageInMiddle,
                    renderPaginationEllipses(),
                    renderPaginationPageButton(page - 1, false, linkForPage),
                    renderPaginationPageButton(page, true, linkForPage),
                    renderPaginationPageButton(page + 1, false, linkForPage),
                    renderPaginationEllipses(),
                    renderPaginationPageButton(pageCount, false, linkForPage))

                /* If the page count is > 7 and the current page is one of the first 4 pages,
                only show the ellipses on the right.
                For example: [1] [!2!] [3] [4] [5] [...] [8] (Current page is 2) */
                .condWith(
                    pageCount > 7 && page <= 4,
                    each(
                        Arrays.asList(new Integer[] {2, 3, 4, 5}),
                        pageNum ->
                            renderPaginationPageButton(pageNum, page == pageNum, linkForPage)),
                    renderPaginationEllipses(),
                    renderPaginationPageButton(pageCount, false, linkForPage))

                /* If the page count is > 7 and the current page is one of the last 4 pages,
                only show the ellipses on the left.
                For example: [1] [...] [4] [!5!] [6] [7] [8] (Current page is 5) */
                .condWith(
                    pageCount > 7 && isCurrentPageInLastFour,
                    renderPaginationEllipses(),
                    each(
                        Arrays.asList(
                            new Integer[] {
                              pageCount - 4, pageCount - 3, pageCount - 2, pageCount - 1
                            }),
                        pageNum ->
                            renderPaginationPageButton(pageNum, page == pageNum, linkForPage)),
                    renderPaginationPageButton(pageCount, page == pageCount, linkForPage))
                .condWith(
                    page != pageCount, renderNextPageButton(page, linkForPage, optionalMessages)));
  }

  private static LiTag renderPaginationPageButton(
      int page, boolean isCurrentPage, Function<Integer, Call> linkForPage) {
    return li().withClass("usa-pagination__item usa-pagination__page-no")
        .with(
            a(Integer.toString(page))
                .withClass("usa-pagination__button")
                .withCondClass(isCurrentPage, "usa-pagination__button usa-current")
                .withHref(linkForPage.apply(page).url())
                .attr("aria-label", "Page" + page)
                .condAttr(isCurrentPage, "aria-current", "page"));
  }

  private static LiTag renderPreviousPageButton(
      int page, Function<Integer, Call> linkForPage, Optional<Messages> optionalMessages) {
    return li().withClass("usa-pagination__item usa-pagination__arrow")
        .with(
            a().withClass("usa-pagination__link usa-pagination__previous-page")
                .attr("aria-label", "Previous page")
                .withHref(linkForPage.apply(page - 1).url())
                .with(
                    Icons.svg(Icons.NAVIGATE_BEFORE)
                        .withClasses("usa-icon", "h-4", "w-4")
                        .attr("role", "img")
                        .attr("aria-hidden", "true"),
                    span(optionalMessages.isPresent()
                            ? optionalMessages
                                .get()
                                .at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName())
                            : "Previous")
                        .withClass("usa-pagination__link-text")));
  }

  private static LiTag renderNextPageButton(
      int page, Function<Integer, Call> linkForPage, Optional<Messages> optionalMessages) {
    return li().withClass("usa-pagination__item usa-pagination__arrow")
        .with(
            a().withClass("usa-pagination__link usa-pagination__next-page")
                .attr("aria-label", "Next page")
                .withHref(linkForPage.apply(page + 1).url())
                .with(
                    span(optionalMessages.isPresent()
                            ? optionalMessages.get().at(MessageKey.BUTTON_NEXT.getKeyName())
                            : "Next")
                        .withClass("usa-pagination__link-text"),
                    Icons.svg(Icons.NAVIGATE_NEXT)
                        .withClasses("usa-icon", "h-4", "w-4")
                        .attr("role", "img")
                        .attr("aria-hidden", "true")));
  }

  private static LiTag renderPaginationEllipses() {
    return li().withClass("usa-pagination__item usa-pagination__overflow")
        .attr("aria-label", "ellipsis indicating non-visible pages")
        .with(span("..."));
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
            span("Note: Fields marked with a ").withClass(BaseStyles.FORM_LABEL_TEXT_COLOR),
            span("*").withClass(BaseStyles.FORM_ERROR_TEXT_COLOR),
            span(" are required.").withClass(BaseStyles.FORM_LABEL_TEXT_COLOR))
        .withClass("text-sm");
  }
}
