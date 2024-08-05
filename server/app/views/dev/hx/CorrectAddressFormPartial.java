package views.dev.hx;

import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import com.google.inject.Inject;
import controllers.dev.routes;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import views.BaseHtmlView;

/** Address checker, create address HTML form. Used on load and via HTMX calls. */
public class CorrectAddressFormPartial extends BaseHtmlView {

  @Inject
  public CorrectAddressFormPartial() {}

  public DivTag render(Http.Request request) {
    return div()
        .withId("correctAddressForm")
        .with(
            h2().withText("Correct Address"),
            form()
                .attr("hx-post", routes.AddressCheckerController.hxCorrectAddress().url())
                .attr("hx-target", "#correctAddressDebugOutput")
                .attr("aria-label", "Find Correction Results")
                .withClass("usa-form")
                .with(
                    fieldset(
                        makeCsrfTokenInputTag(request),
                        label().withText("Address 1").withClasses("usa-label").withFor("address1"),
                        input().withId("address1").withName("address1").withClasses("usa-input"),
                        label().withText("Address 2").withClasses("usa-label").withFor("address2"),
                        input().withId("address2").withName("address2").withClasses("usa-input"),
                        label().withText("City").withClasses("usa-label").withFor("city"),
                        input().withId("city").withName("city").withClasses("usa-input"),
                        label().withText("State").withClasses("usa-label").withFor("state"),
                        input().withId("state").withName("state").withClasses("usa-input"),
                        label().withText("Zip Code").withClasses("usa-label").withFor("zip"),
                        input().withId("zip").withName("zip").withClasses("usa-input"),
                        div()
                            .withClasses("mt-4")
                            .with(
                                submitButton("Correct Address").withClasses("usa-button"),
                                button("Reset")
                                    .withType("reset")
                                    .withClasses("usa-button usa-button--outline")))));
  }
}
