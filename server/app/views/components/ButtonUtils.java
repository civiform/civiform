package views.components;

import static j2html.TagCreator.button;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.span;

import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import play.filters.csrf.CSRF;
import play.mvc.Http;
import scala.Option;
import views.style.Styles;

/** Contains helpers for creating buttons for use in the UI. */
public final class ButtonUtils {

  public static ContainerTag asLinkButtonForPost(
      ContainerTag el, String href, Http.Request request) {
    Option<CSRF.Token> csrfTokenMaybe = CSRF.getToken(request.asScala());
    String csrfToken = "";
    if (csrfTokenMaybe.isDefined()) {
      csrfToken = csrfTokenMaybe.get().value();
    }

    String formId = RandomStringUtils.randomAlphabetic(32);
    Tag hiddenForm =
        form()
            .withId(formId)
            .withClass(Styles.HIDDEN)
            .withMethod("POST")
            .withAction(href)
            .with(input().isHidden().withValue(csrfToken).withName("csrfToken"));

    return el.attr("form", formId).with(hiddenForm);
  }

  public static ContainerTag asLinkButton(ContainerTag el, String href) {
    return el.attr("onclick", String.format("document.location.href = \"%s\"", href));
  }

  public static ContainerTag makeSvgTextButton(String buttonText, String svgPath) {
    return button()
        .with(
            Icons.svg(svgPath, 18).withClasses(Styles.ML_1, Styles.MR_2, Styles.INLINE_BLOCK),
            span(buttonText));
  }
}
