package views.dev.hx;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;

public class ServiceAreaResultsPartialViewModelTest {

  private static final ServiceAreaInclusion SEATTLE =
      ServiceAreaInclusion.create("Seattle", ServiceAreaState.IN_AREA, 0L);
  private static final ServiceAreaInclusion BLOOMINGTON =
      ServiceAreaInclusion.create("Bloomington", ServiceAreaState.NOT_IN_AREA, 0L);

  @Test
  public void selectedResult_matchingOption_returnsIt() {
    var model =
        new ServiceAreaResultsPartialViewModel(
            "Bloomington", ImmutableList.of(SEATTLE, BLOOMINGTON));

    assertThat(model.selectedResult()).contains(BLOOMINGTON);
  }

  @Test
  public void selectedResult_noMatch_isEmpty() {
    var model = new ServiceAreaResultsPartialViewModel("Portland", ImmutableList.of(SEATTLE));

    assertThat(model.selectedResult()).isEmpty();
  }

  @Test
  public void isInArea_checksState() {
    var model =
        new ServiceAreaResultsPartialViewModel("Seattle", ImmutableList.of(SEATTLE, BLOOMINGTON));

    assertThat(model.isInArea(SEATTLE)).isTrue();
    assertThat(model.isInArea(BLOOMINGTON)).isFalse();
  }

  @Test
  public void isSelected_matchesValidationOption() {
    var model =
        new ServiceAreaResultsPartialViewModel("Seattle", ImmutableList.of(SEATTLE, BLOOMINGTON));

    assertThat(model.isSelected(SEATTLE)).isTrue();
    assertThat(model.isSelected(BLOOMINGTON)).isFalse();
  }
}
