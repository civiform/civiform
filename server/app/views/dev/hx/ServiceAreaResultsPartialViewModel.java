package views.dev.hx;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import views.BaseViewModel;

/** Contains all the properties for rendering the ServiceAreaResultsPartial.html */
public record ServiceAreaResultsPartialViewModel(
    String validationOption, ImmutableList<ServiceAreaInclusion> serviceAreaInclusions)
    implements BaseViewModel {

  /** The result for the service area the user picked, if the response included it */
  public Optional<ServiceAreaInclusion> selectedResult() {
    return serviceAreaInclusions.stream()
        .filter(inclusion -> inclusion.getServiceAreaId().equals(validationOption))
        .findFirst();
  }

  public boolean isInArea(ServiceAreaInclusion inclusion) {
    return inclusion.getState() == ServiceAreaState.IN_AREA;
  }

  public boolean isSelected(ServiceAreaInclusion inclusion) {
    return inclusion.getServiceAreaId().equals(validationOption);
  }
}
