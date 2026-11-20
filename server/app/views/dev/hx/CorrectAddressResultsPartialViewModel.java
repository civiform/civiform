package views.dev.hx;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import services.geo.AddressSuggestion;
import services.geo.esri.EsriServiceAreaValidationOption;
import views.BaseViewModel;

/** Contains all the properties for rendering the CorrectAddressResultsPartial.html */
@Builder
public record CorrectAddressResultsPartialViewModel(
    ImmutableList<AddressSuggestion> addressSuggestions,
    boolean serviceAreaValidationEnabled,
    ImmutableMap<String, EsriServiceAreaValidationOption> serviceAreaValidationOptions,
    String checkServiceAreaActionUrl)
    implements BaseViewModel {}
