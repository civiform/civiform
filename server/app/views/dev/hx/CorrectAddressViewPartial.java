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
import static j2html.TagCreator.table;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import controllers.dev.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import java.util.UUID;
import play.mvc.Http;
import services.geo.AddressLocation;
import services.geo.AddressSuggestionGroup;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.settings.SettingsManifest;
import views.BaseHtmlView;

/** Address checker, create address results. Called via HTMX. */
public class CorrectAddressViewPartial extends BaseHtmlView {
  private EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

  public DivTag render(
      Http.Request request,
      SettingsManifest settingsManifest,
      AddressSuggestionGroup addressSuggestionGroup,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig) {
    checkNotNull(request);
    checkNotNull(settingsManifest);
    checkNotNull(addressSuggestionGroup);
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);

    DivTag divTag = div();

    if (addressSuggestionGroup.getAddressSuggestions().isEmpty()) {
      return divTag.with(
          h2().withText("Address Correction Results"), div("No results found").withClasses("mt-4"));
    }

    divTag.with(h2().withText("Address Correction Results"));

    for (var addressSuggestion : addressSuggestionGroup.getAddressSuggestions()) {
      AddressLocation location = addressSuggestion.getLocation();
      String idSuffix = UUID.randomUUID().toString().substring(0, 7);

      divTag
          .withData("testid", "address-correction-results")
          .withClasses("space-y-4")
          .with(
              form()
                  .attr("hx-post", routes.AddressCheckerController.hxCheckServiceArea().url())
                  .attr("hx-target", "#checkServiceAreaDebugOutput")
                  .withId("correction-form-" + idSuffix)
                  .withClasses("usa-form")
                  .with(
                      h2(addressSuggestion.getAddress().getStreet()).withClasses("font-bold"),
                      div()
                          .with(
                              fieldset(
                                      div()
                                          .with(
                                              makeCsrfTokenInputTag(request),
                                              div(String.format(
                                                      "%s, %s, %s",
                                                      addressSuggestion.getAddress().getCity(),
                                                      addressSuggestion.getAddress().getState(),
                                                      addressSuggestion.getAddress().getZip()))
                                                  .withClasses("mb-2"),
                                              table(
                                                      tr(
                                                          th("Latitude/Y")
                                                              .withClasses("text-left", "pr-8"),
                                                          td(location.getLatitude().toString())),
                                                      tr(
                                                          th("Longitude/X")
                                                              .withClasses("text-left"),
                                                          td(location.getLongitude().toString())),
                                                      tr(
                                                          th("WellKnownId")
                                                              .withClasses("text-left"),
                                                          td(location.getWellKnownId().toString())),
                                                      tr(
                                                          th("Score").withClasses("text-left"),
                                                          td(addressSuggestion.getScore() + "")))
                                                  .withClasses("table-fixed"),
                                              input()
                                                  .withType("hidden")
                                                  .withId("latitude-" + idSuffix)
                                                  .withName("latitude")
                                                  .withValue(location.getLatitude().toString()),
                                              input()
                                                  .withType("hidden")
                                                  .withId("longitude-" + idSuffix)
                                                  .withName("longitude")
                                                  .withValue(location.getLongitude().toString()),
                                              input()
                                                  .withType("hidden")
                                                  .withId("wellKnownId-" + idSuffix)
                                                  .withName("wellKnownId")
                                                  .withValue(location.getWellKnownId().toString())))
                                  .condWith(
                                      settingsManifest.getEsriAddressServiceAreaValidationEnabled(
                                          request),
                                      div()
                                          .with(
                                              label()
                                                  .with(
                                                      div("Validation").withClasses("font-bold"),
                                                      div("Format: label (attribute=id)")
                                                          .withClasses("italic"))
                                                  .withClasses("usa-label")
                                                  .withFor("validationOption-" + idSuffix),
                                              select()
                                                  .withId("validationOption-" + idSuffix)
                                                  .withName("validationOption")
                                                  .withClasses("usa-select")
                                                  .with(
                                                      renderServiceAreaValidationConfigSelectOptions()),
                                              submitButton("Check Service Area")
                                                  .withClasses("mt-4", "usa-button"))))));
    }

    return divTag;
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
