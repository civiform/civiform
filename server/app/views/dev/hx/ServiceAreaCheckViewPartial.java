package views.dev.hx;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.span;
import static j2html.TagCreator.table;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import views.BaseHtmlView;

/** Address checker, search service area results. Called via HTMX. */
public class ServiceAreaCheckViewPartial extends BaseHtmlView {
  public DivTag render(
      String validationOption, ImmutableList<ServiceAreaInclusion> addressSuggestionGroup) {

    return div()
        .with(
            renderValidationResult(validationOption, addressSuggestionGroup),
            renderTable(validationOption, addressSuggestionGroup));
  }

  private DivTag renderValidationResult(
      String validationOption, ImmutableList<ServiceAreaInclusion> addressSuggestionGroup) {
    Optional<ServiceAreaInclusion> validationOptionResult =
        addressSuggestionGroup.stream()
            .filter(x -> x.getServiceAreaId().equals(validationOption))
            .findFirst();

    return div()
        .condWith(
            validationOptionResult.isPresent(),
            h2().withText("Validation Result: ")
                .with(
                    span(validationOptionResult.get().getState().getSerializationFormat())
                        .withCondClass(
                            validationOptionResult.get().getState() == ServiceAreaState.IN_AREA,
                            "text-green-700")
                        .withCondClass(
                            validationOptionResult.get().getState() != ServiceAreaState.IN_AREA,
                            "text-red-700")));
  }

  private DivTag renderTable(
      String validationOption, ImmutableList<ServiceAreaInclusion> addressSuggestionGroup) {
    var divTag = div().with(h3("Result Details")).withClasses("mt-4");

    var tableTag =
        table(
                tr(
                    th("ServiceAreaId")
                        .withClasses("text-left", "border", "border-gray-600", "p-2"),
                    th("State").withClasses("text-left", "border", "border-gray-600", "p-2")))
            .withClasses("border-collapse", "border", "border-gray-600", "w-96", "table-auto");

    for (ServiceAreaInclusion serviceAreaInclusion : addressSuggestionGroup) {
      tableTag.with(
          tr(
                  td(serviceAreaInclusion.getServiceAreaId())
                      .withClasses("border", "border-gray-600", "p-2"),
                  td(serviceAreaInclusion.getState().getSerializationFormat())
                      .withClasses("border", "border-gray-600", "p-2"))
              .withCondClass(
                  !validationOption.equals(serviceAreaInclusion.getServiceAreaId()),
                  "text-gray-500"));
    }

    divTag.with(tableTag);

    return divTag;
  }
}
