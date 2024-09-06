package views.errors;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.ul;

import controllers.routes;
import j2html.tags.UnescapedText;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;

/**
 * Render an error component based on the USWDS 404 page template
 *
 * @see <a
 *     href="https://designsystem.digital.gov/templates/404-page">https://designsystem.digital.gov/templates/404-page</a>
 */
public final class ErrorComponent {

  public static DivTag renderErrorComponent(
      String title,
      Optional<String> subtitle,
      Optional<UnescapedText> additionalInfo,
      String buttonText,
      Messages messages,
      Optional<String> statusCode) {

    DivTag button =
        div()
            .withClass("margin-y-5")
            .with(
                ul().withClass("usa-button-group")
                    .with(
                        li().withClass("usa-button-group__item")
                            .with(
                                a().withClass("usa-button")
                                    .withId("visit-home-page-button")
                                    .withText(buttonText)
                                    .withHref(routes.HomeController.index().url()))));

    return div()
        .with(
            div()
                .withClass("usa-section")
                .with(
                    div()
                        .withClass("grid-container")
                        .with(
                            div()
                                .withClasses("grid-row", "grid-gap")
                                .with(
                                    div()
                                        .withId("main-content")
                                        .withClasses("usa-prose")
                                        .with(h1(title).withClasses("mb-4"))
                                        .condWith(
                                            subtitle.isPresent(),
                                            p().withClass("usa-intro")
                                                .withText(subtitle.orElse("")))
                                        .condWith(
                                            additionalInfo.isPresent(),
                                            div(additionalInfo.orElse(rawHtml(""))))
                                        .with(
                                            div()
                                                .withClass("margin-y-5")
                                                .with(button)
                                                .condWith(
                                                    statusCode.isPresent(),
                                                    p(messages.at(
                                                            MessageKey.ERROR_STATUS_CODE
                                                                .getKeyName(),
                                                            statusCode.orElse("")))
                                                        .withClass("text-base")))))));
  }
}
