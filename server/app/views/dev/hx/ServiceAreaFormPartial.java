package views.dev.hx;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.dev.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import play.mvc.Http;
import services.geo.esri.EsriServiceAreaValidationConfig;
import views.BaseHtmlView;

/** Address checker, search service area results HTML form. Called via HTMX. */
public class ServiceAreaFormPartial extends BaseHtmlView {
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

  @Inject
  public ServiceAreaFormPartial(EsriServiceAreaValidationConfig esriServiceAreaValidationConfig) {
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
  }

  public DivTag render(Http.Request request) {
    return div()
        .withId("serviceAreaForm")
        .with(
            h2().withText("Check Service Area"),
            form()
                .attr("hx-post", routes.AddressCheckerController.hxCheckServiceArea().url())
                .attr("hx-target", "#checkServiceAreaDebugOutput")
                .attr("aria-label", "Search Service Area")
                .withClass("usa-form")
                .with(
                    fieldset(
                        makeCsrfTokenInputTag(request),
                        label().withText("Latitude/Y").withClasses("usa-label").withFor("latitude"),
                        input()
                            .withId("latitude")
                            .withName("latitude")
                            .withClasses("usa-input")
                            .isRequired(),
                        label()
                            .withText("Longitude/X")
                            .withClasses("usa-label")
                            .withFor("longitude"),
                        input()
                            .withId("longitude")
                            .withName("longitude")
                            .withClasses("usa-input")
                            .isRequired(),
                        label()
                            .withText("WellKnownId")
                            .withClasses("usa-label")
                            .withFor("wellKnownId"),
                        input()
                            .withId("wellKnownId")
                            .withName("wellKnownId")
                            .withClasses("usa-input")
                            .isRequired(),
                        label()
                            .with(
                                div("Validation"),
                                div("Format: label (attribute=id)").withClasses("italic"))
                            .withClasses("usa-label")
                            .withFor("validationOption"),
                        select()
                            .withId("validationOption")
                            .withName("validationOption")
                            .withClasses("usa-select")
                            .isRequired()
                            .with(renderServiceAreaValidationConfigSelectOptions()),
                        div()
                            .withClasses("mt-4")
                            .with(
                                submitButton("Check Service Area").withClasses("usa-button"),
                                button("Reset")
                                    .withType("reset")
                                    .withClasses("usa-button usa-button--outline")))));
  }

  private ImmutableList<OptionTag> renderServiceAreaValidationConfigSelectOptions() {
    var serviceAreaValidationConfigImmutableMap = esriServiceAreaValidationConfig.getImmutableMap();

    ImmutableList.Builder<OptionTag> optionTagBuilder = ImmutableList.builder();

    serviceAreaValidationConfigImmutableMap.forEach(
        (key, value) -> {
          optionTagBuilder.add(
              option()
                  .withText(
                      String.format(
                          "%s (%s=%s)", value.getLabel(), value.getAttribute(), value.getId()))
                  .withValue(key));
        });

    return optionTagBuilder.build();
  }
}
