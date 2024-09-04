package views.errors;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.ul;

import java.util.Optional;
import controllers.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.PTag;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import views.BaseHtmlView;
import views.HtmlBundle;

public final class ErrorComponent extends BaseHtmlView {

// must have a title, subtitle, button text, button link
// optional message and status code
  public static DivTag renderErrorComponent(String title, String subtitle, Optional<String> additionalInfo, String buttonText, String buttonLink, Optional<String> statusCode) {
    H1Tag headerText =
        renderHeader(title);
    PTag contentText =
        p().withClass("usa-intro")
            .withText(subtitle);

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
                                    .withHref(buttonLink))));

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
                                        .with(headerText)
                                        .with(contentText)
                                        .with(div().withClass("margin-y-5").with(button))))));
  }
}
